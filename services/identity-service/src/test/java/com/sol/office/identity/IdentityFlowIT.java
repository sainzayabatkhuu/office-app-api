package com.sol.office.identity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * End-to-end through the real schema (Flyway + Hibernate validate) on MySQL.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class IdentityFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @Test
    void adminCanSignInAndSeeProfile() throws Exception {
        JsonNode tokens = login("admin", "Admin@123");
        assertThat(tokens.get("mustChangePassword").asBoolean()).isFalse();

        mvc.perform(get("/api/v1/auth/me").header("Authorization", bearer(tokens)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("admin"))
                .andExpect(jsonPath("$.branchCode").value("000"))
                .andExpect(jsonPath("$.roles[0]").value("ADMIN"))
                .andExpect(jsonPath("$.privileges").isArray());
    }

    @Test
    void requestsWithoutTokenAreRejected() throws Exception {
        mvc.perform(get("/api/v1/employees"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
        mvc.perform(get("/.well-known/jwks.json"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys[0].kty").value("RSA"));
    }

    @Test
    void employeeGetsAccountChangesPasswordAndRefreshTokenRotates() throws Exception {
        String admin = bearer(login("admin", "Admin@123"));

        // 1. HR creates the employee
        JsonNode employee = body(mvc.perform(post("/api/v1/employees").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "employeeCode", "E10001", "lastName", "Батхүү", "firstName", "Сайнзаяа",
                        "registerNo", "УБ90010101", "email", "sainzaya@office.local",
                        "hireDate", "2024-01-15", "branchId", 1))))
                .andExpect(status().isCreated()));
        assertThat(employee.get("displayName").asText()).isEqualTo("Б.Сайнзаяа");

        // 2. Admin grants a sign-in account (app_user shares the employee id)
        JsonNode account = body(mvc.perform(post("/api/v1/users").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "employeeId", employee.get("id").asLong(), "username", "sainzaya",
                        "roleCodes", new String[]{"REPORT_USER"}, "multiBranchAccess", false))))
                .andExpect(status().isCreated()));
        String tempPassword = account.get("temporaryPassword").asText();

        // 3. First login must change the password
        JsonNode first = login("sainzaya", tempPassword);
        assertThat(first.get("mustChangePassword").asBoolean()).isTrue();
        mvc.perform(put("/api/v1/auth/me/password").header("Authorization", bearer(first))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("currentPassword", tempPassword, "newPassword", "NewPass123"))))
                .andExpect(status().isOk());

        // A report user may not manage employees
        JsonNode user = login("sainzaya", "NewPass123");
        mvc.perform(get("/api/v1/employees").header("Authorization", bearer(user)))
                .andExpect(status().isForbidden());

        // 4. Refresh rotates; re-using the old refresh token revokes the whole session family
        String oldRefresh = user.get("refreshToken").asText();
        JsonNode rotated = body(refresh(oldRefresh).andExpect(status().isOk()));
        refresh(oldRefresh).andExpect(status().isUnauthorized());
        refresh(rotated.get("refreshToken").asText()).andExpect(status().isUnauthorized());
    }

    @Test
    void repeatedFailuresLockTheAccountTemporarily() throws Exception {
        String admin = bearer(login("admin", "Admin@123"));
        JsonNode employee = body(mvc.perform(post("/api/v1/employees").header("Authorization", admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of(
                        "employeeCode", "E10002", "lastName", "Дорж", "firstName", "Бат",
                        "hireDate", "2023-05-01", "branchId", 1))))
                .andExpect(status().isCreated()));
        mvc.perform(post("/api/v1/users").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "employeeId", employee.get("id").asLong(), "username", "bat",
                                "password", "Correct123", "roleCodes", new String[]{"REPORT_USER"}))))
                .andExpect(status().isCreated());

        for (int i = 0; i < 3; i++) {
            attemptLogin("bat", "wrong-password").andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        }
        attemptLogin("bat", "Correct123")
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_TEMPORARILY_LOCKED"));
    }

    private JsonNode login(String username, String password) throws Exception {
        return body(attemptLogin(username, password).andExpect(status().isOk()));
    }

    private ResultActions attemptLogin(String username, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password))));
    }

    private ResultActions refresh(String refreshToken) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", refreshToken))));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString());
    }

    private static String bearer(JsonNode tokens) {
        return "Bearer " + tokens.get("accessToken").asText();
    }
}
