package com.sol.office.report;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sol.office.report.client.IdentityClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Upload -> permission -> SYNC and ASYNC generation -> download, against real MySQL and RabbitMQ.
 * The sample template queries report_category, so the service's own schema doubles as the reporting database.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Testcontainers
class ReportFlowIT {

    @Container
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-management");

    @DynamicPropertySource
    static void datasources(DynamicPropertyRegistry registry) {
        for (String prefix : List.of("spring.datasource", "office.report.datasource")) {
            registry.add(prefix + ".url", mysql::getJdbcUrl);
            registry.add(prefix + ".username", mysql::getUsername);
            registry.add(prefix + ".password", mysql::getPassword);
        }
        registry.add("office.report.storage.base-dir", () -> System.getProperty("java.io.tmpdir") + "/office-report-it-" + System.nanoTime());
    }

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @MockitoBean
    IdentityClient identityClient;

    @Test
    void fullReportLifecycle() throws Exception {
        // 1. Report admin uploads the template; parameters are extracted, server-filled ones flagged
        JsonNode created = body(mvc.perform(multipart("/api/v1/reports")
                        .file(new MockMultipartFile("file", "report_categories.jrxml", "application/xml",
                                new ClassPathResource("reports/report_categories.jrxml").getInputStream()))
                        .file(new MockMultipartFile("report", "", MediaType.APPLICATION_JSON_VALUE, json.writeValueAsBytes(Map.of(
                                "code", "REPORT_CATEGORIES",
                                "metadata", Map.of("title", "Тайлангийн ангилал", "allowedFormats", List.of("PDF", "XLSX", "CSV"),
                                        "defaultFormat", "PDF", "executionMode", "SYNC", "active", true)))))
                        .with(user("reportadmin", List.of("REPORT_ADMIN"), "REPORT_MANAGE", "REPORT_VIEW", "REPORT_EXECUTE")))
                .andExpect(status().isCreated()));
        long reportId = created.at("/report/id").asLong();
        assertThat(created.at("/parameters").toString())
                .contains("\"name\":\"minSortOrder\"", "\"defaultValue\":\"0\"", "\"label\":\"Хамгийн бага эрэмбэ\"");
        assertThat(created.at("/permissions/0/roleCode").asText()).isEqualTo("ADMIN");

        // 2. A report user cannot see it until their role is granted
        RequestPostProcessor reportUser = user("bat", List.of("REPORT_USER"), "REPORT_VIEW", "REPORT_EXECUTE");
        mvc.perform(get("/api/v1/reports").with(reportUser)).andExpect(jsonPath("$.length()").value(0));
        mvc.perform(post("/api/v1/reports/{id}/executions", reportId).with(reportUser)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());

        when(identityClient.listRoles()).thenReturn(List.of(new IdentityClient.RoleSummary(3L, "REPORT_USER", "Report user")));
        mvc.perform(post("/api/v1/reports/{id}/permissions", reportId)
                        .with(user("reportadmin", List.of("REPORT_ADMIN"), "REPORT_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"roleCode\":\"REPORT_USER\"}"))
                .andExpect(status().isCreated());

        // the user only sees parameters they fill in (not reportTitle/maker)
        JsonNode detail = body(mvc.perform(get("/api/v1/reports/{id}", reportId).with(reportUser)).andExpect(status().isOk()));
        assertThat(detail.at("/parameters").size()).isEqualTo(1);

        // 3. SYNC PDF: generated within the request
        JsonNode pdf = body(execute(reportId, reportUser, "PDF", Map.of("minSortOrder", "20")).andExpect(status().isOk()));
        assertThat(pdf.get("status").asText()).isEqualTo("SUCCESS");
        assertThat(pdf.get("parameters").get("maker").asText()).isEqualTo("bat");
        byte[] pdfBytes = mvc.perform(get(pdf.get("downloadUrl").asText()).with(reportUser))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(Arrays.copyOf(pdfBytes, 4), StandardCharsets.US_ASCII)).isEqualTo("%PDF");

        // CSV contains Cyrillic data and respects the parameter (sort_order >= 20 excludes GENERAL)
        JsonNode csv = body(execute(reportId, reportUser, "CSV", Map.of("minSortOrder", "20")).andExpect(status().isOk()));
        String csvText = mvc.perform(get(csv.get("downloadUrl").asText()).with(reportUser))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        assertThat(csvText).contains("Хүний нөөц").doesNotContain("Ерөнхий");

        // invalid parameter is rejected before anything runs; disallowed format too
        execute(reportId, reportUser, "PDF", Map.of("minSortOrder", "abc"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));
        execute(reportId, reportUser, "DOCX", Map.of()).andExpect(jsonPath("$.errorCode").value("FORMAT_NOT_ALLOWED"));

        // other users cannot download it
        mvc.perform(get(pdf.get("downloadUrl").asText()).with(user("dorj", List.of("REPORT_USER"), "REPORT_VIEW")))
                .andExpect(status().isNotFound());

        // 4. ASYNC: queued on RabbitMQ, processed by the listener
        mvc.perform(put("/api/v1/reports/{id}", reportId)
                        .with(user("reportadmin", List.of("REPORT_ADMIN"), "REPORT_MANAGE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("title", "Тайлангийн ангилал", "allowedFormats", List.of("PDF", "XLSX"),
                                "defaultFormat", "XLSX", "executionMode", "ASYNC", "active", true))))
                .andExpect(status().isOk());
        JsonNode queued = body(execute(reportId, reportUser, null, Map.of()).andExpect(status().isAccepted()));
        assertThat(queued.get("status").asText()).isEqualTo("PENDING");

        String executionId = queued.get("id").asText();
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                mvc.perform(get("/api/v1/report-executions/{id}", executionId).with(reportUser))
                        .andExpect(jsonPath("$.status").value("SUCCESS"))
                        .andExpect(jsonPath("$.format").value("XLSX")));

        // 5. History: own executions only, unless REPORT_HISTORY_ALL
        mvc.perform(get("/api/v1/report-executions").with(reportUser))
                .andExpect(jsonPath("$.totalElements").value(3));
        mvc.perform(get("/api/v1/report-executions").param("all", "true").with(reportUser))
                .andExpect(status().isForbidden());
    }

    private ResultActions execute(long reportId, RequestPostProcessor user, String format, Map<String, String> params) throws Exception {
        Map<String, Object> body = format == null ? Map.of("parameters", params) : Map.of("format", format, "parameters", params);
        return mvc.perform(post("/api/v1/reports/{id}/executions", reportId).with(user)
                .contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body)));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** A token shaped like identity-service's, without needing identity-service. */
    private static RequestPostProcessor user(String username, List<String> roles, String... privileges) {
        return jwt().jwt(j -> j.subject(username)
                        .claim("uid", 1L)
                        .claim("name", username)
                        .claim("branch", "000")
                        .claim("mba", false)
                        .claim("roles", roles)
                        .claim("authorities", List.of(privileges)))
                .authorities(Arrays.stream(privileges).<GrantedAuthority>map(SimpleGrantedAuthority::new).toList());
    }
}
