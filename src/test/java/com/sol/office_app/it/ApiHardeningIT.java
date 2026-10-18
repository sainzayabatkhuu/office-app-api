package com.sol.office_app.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sol.office_app.entity.Branch;
import com.sol.office_app.entity.Privilege;
import com.sol.office_app.entity.Role;
import com.sol.office_app.entity.User;
import com.sol.office_app.entity.UserRoles;
import com.sol.office_app.repository.BranchRepository;
import com.sol.office_app.repository.PrivilegeRepository;
import com.sol.office_app.repository.RoleRepository;
import com.sol.office_app.repository.UserRepository;
import com.sol.office_app.repository.UserRolesRepository;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the API changes made for the new front-end against a real MySQL (schema from ddl-auto).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "security.jwt.secret-key=integration-test-secret-key-0123456789abcdef",
        "security.jwt.login.attemps=3",
        "spring.config.import=",
        "app.cors.allowed-origins=http://localhost:4200"
})
@AutoConfigureMockMvc
@Testcontainers
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ApiHardeningIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    static Path workDir;

    @DynamicPropertySource
    static void dirs(DynamicPropertyRegistry registry) throws Exception {
        workDir = Files.createTempDirectory("office-it");
        registry.add("report.subreport.dir", () -> workDir.resolve("jrxml") + "/");
        registry.add("report.output.dir", () -> workDir.resolve("out") + "/");
    }

    @LocalServerPort int port;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired UserRepository users;
    @Autowired UserRolesRepository userRoles;
    @Autowired RoleRepository roles;
    @Autowired PrivilegeRepository privileges;
    @Autowired BranchRepository branches;
    @Autowired PasswordEncoder encoder;
    @Autowired TransactionTemplate tx;

    Long viewerRoleId;

    /**
     * admin   - every privilege below
     * viewer  - read roles, read/run reports (rule with several comma-separated entries), own history only
     * teller  - nothing
     */
    @BeforeAll
    void seed() {
        tx.executeWithoutResult(s -> {
            branches.save(new Branch("000", "Head office", "HO", true));
            branches.save(new Branch("101", "Branch 101", "B101", true));
            List<Privilege> all = new ArrayList<>();
            for (String[] p : new String[][]{
                    {"get:/roles", "View Roles"}, {"post:/roles", "Create Roles"},
                    {"get:/users", "View Users"}, {"post:/users", "Create Users"}, {"put:/users", "Update Users"},
                    {"get:/branches", "View Branches"}, {"post:/branches", "Create Branches"},
                    {"get:/reports/*,get:/reports/report-list,get:/reports/details/*", "View Reports"},
                    {"post:/reports", "Create Reports"},
                    {"get:/report-permissions", "View Report Permissions"},
                    {"post:/report-permissions", "Create Report Permissions"},
                    {"put:/report-permissions", "Update Report Permissions"},
                    {"delete:/report-permissions", "Delete Report Permissions"},
                    {"get:/report-histories", "View Background History"},
                    {"get:/reports/export", "Generate Report"}}) {
                Privilege priv = new Privilege();
                priv.setValue(p[0]);
                priv.setDescription(p[1]);
                all.add(privileges.save(priv));
            }
            Role admin = new Role("ROLE_ADMIN");
            admin.setPrivileges(all);
            roles.save(admin);

            Role viewer = new Role("ROLE_REPORT_VIEWER");
            viewer.setPrivileges(all.stream().filter(p -> p.getValue().startsWith("get:/roles")
                    || p.getValue().startsWith("get:/reports")).toList());
            viewerRoleId = roles.save(viewer).getId();

            createUser("admin", "Admin123", admin);
            createUser("viewer", "Viewer123", viewer);
            createUser("teller", "Teller123", null);
        });
    }

    private void createUser(String username, String password, Role role) {
        User u = new User();
        u.setUsername(username);
        u.setEmail(username + "@office.local");
        u.setPassword(encoder.encode(password));
        u.setIsActive(true);
        u.setAccountNonLocked(true);
        u.setMultiBrnchAccess(false);
        u.setFontSize("font-s");
        u.setLastPasswordChangeDate(LocalDate.now().plusDays(90));
        u.setBranch(branches.findById("000").orElseThrow());
        users.save(u);
        if (role != null) {
            UserRoles link = new UserRoles();
            link.setUser(u);
            link.setRole(role);
            link.setBranch(u.getBranch());
            userRoles.save(link);
        }
    }

    // ------------------------------------------------------------------ authentication

    @Test
    void loginIssuesShortLivedAccessTokenAndRefreshToken() throws Exception {
        JsonNode tokens = login("admin", "Admin123");
        assertThat(tokens.get("refreshToken").asText()).hasSizeGreaterThan(30);
        assertThat(tokens.get("expiresIn").asLong()).isEqualTo(1_800_000L);
    }

    @Test
    void unknownUserAndWrongPasswordLookTheSame() throws Exception {
        attemptLogin("nobody", "x").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"));
        attemptLogin("teller", "wrong").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CREDENTIALS"))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void repeatedFailuresLockTemporarily() throws Exception {
        tx.executeWithoutResult(s -> createUser("locky", "Locky123", null));
        for (int i = 0; i < 3; i++) {
            attemptLogin("locky", "bad");
        }
        attemptLogin("locky", "Locky123").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("ACCOUNT_TEMPORARILY_LOCKED"));
    }

    @Test
    void refreshRotatesAndReuseRevokesTheSession() throws Exception {
        String first = login("viewer", "Viewer123").get("refreshToken").asText();
        JsonNode second = body(refresh(first).andExpect(status().isOk()));
        refresh(first).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.errorCode").value("INVALID_REFRESH_TOKEN"));
        // reuse of the old token killed the new one too
        refresh(second.get("refreshToken").asText()).andExpect(status().isUnauthorized());
    }

    // ------------------------------------------------------------------ authorization

    @Test
    void privilegeRulesGuardEveryEndpoint() throws Exception {
        String viewer = bearer("viewer", "Viewer123");
        mvc.perform(get("/api/v1/roles").header("Authorization", viewer)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/roles/" + viewerRoleId).header("Authorization", viewer)).andExpect(status().isOk());
        mvc.perform(post("/api/v1/roles").header("Authorization", viewer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"X\"}"))
                .andExpect(status().isForbidden()).andExpect(jsonPath("$.errorCode").value("FORBIDDEN"));
        mvc.perform(get("/api/v1/users").header("Authorization", viewer)).andExpect(status().isForbidden());
        // second entry of a comma-separated privilege value
        mvc.perform(get("/api/v1/reports/report-list").header("Authorization", viewer)).andExpect(status().isOk());
        // self-service needs no privilege
        mvc.perform(get("/api/v1/users/me").header("Authorization", bearer("teller", "Teller123")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.username").value("teller"));
        mvc.perform(get("/api/v1/roles")).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.errorCode").value("UNAUTHENTICATED"));
    }

    // ------------------------------------------------------------------ branches & users

    @Test
    void branchesCanBeFetchedAndCreatedByCode() throws Exception {
        String admin = bearer("admin", "Admin123");
        mvc.perform(get("/api/v1/branches/101").header("Authorization", admin))
                .andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Branch 101"));
        mvc.perform(post("/api/v1/branches").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solId\":\"202\",\"name\":\"Branch 202\",\"branchAvailable\":true}"))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/branches").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"solId\":\"202\",\"name\":\"Again\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void userLifecycleWithTemporaryPasswords() throws Exception {
        String admin = bearer("admin", "Admin123");
        JsonNode created = body(mvc.perform(post("/api/v1/users").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"bat\",\"email\":\"bat@office.local\",\"firstname\":\"Бат\","
                                + "\"branchCode\":\"101\",\"roleNames\":[\"ROLE_REPORT_VIEWER\"]}"))
                .andExpect(status().isCreated()));
        String temp = created.get("temporaryPassword").asText();

        String bat = "Bearer " + login("bat", temp).get("accessToken").asText();
        mvc.perform(get("/api/v1/users/me").header("Authorization", bat))
                .andExpect(jsonPath("$.forcePasswordChange").value(true))
                .andExpect(jsonPath("$.branchCode").value("101"));
        mvc.perform(put("/api/v1/users/change-password").header("Authorization", bat).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + temp + "\",\"password\":\"short\",\"confirmPassword\":\"short\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("WEAK_PASSWORD"));
        mvc.perform(put("/api/v1/users/change-password").header("Authorization", bat).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"" + temp + "\",\"password\":\"Better123\",\"confirmPassword\":\"Better123\"}"))
                .andExpect(status().isOk());
        mvc.perform(put("/api/v1/users/me").header("Authorization", bat).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"userLanguage\":\"en\"}"))
                .andExpect(jsonPath("$.userLanguage").value("en"))
                .andExpect(jsonPath("$.forcePasswordChange").value(false));

        Long id = users.findByUsername("bat").orElseThrow().getId();
        JsonNode reset = body(mvc.perform(put("/api/v1/users/" + id + "/reset-password").header("Authorization", admin))
                .andExpect(status().isOk()));
        login("bat", reset.get("temporaryPassword").asText());
        mvc.perform(get("/api/v1/users/" + id).header("Authorization", admin))
                .andExpect(jsonPath("$.roleNames[0]").value("ROLE_REPORT_VIEWER"))
                .andExpect(jsonPath("$.branchCode").value("101"));
    }

    // ------------------------------------------------------------------ reports

    @Test
    void reportRunsDownloadsAndRunsInBackgroundWithNotification() throws Exception {
        String admin = bearer("admin", "Admin123");
        JsonNode report = body(mvc.perform(multipart("/api/v1/reports").file(new MockMultipartFile("file", "it_sample.jrxml",
                                "application/xml", new ClassPathResource("reports/it_sample.jrxml").getInputStream()))
                        .param("title", "Тест тайлан").header("Authorization", admin))
                .andExpect(status().isOk()));
        long reportId = report.get("id").asLong();

        // immediate (V) run by admin, then download
        JsonNode run = body(mvc.perform(get("/api/v1/reports/export").header("Authorization", admin)
                        .param("id", String.valueOf(reportId)).param("format", "pdf").param("p_count", "7"))
                .andExpect(status().isOk()));
        String historyId = run.at("/data/historyId").asText();
        byte[] pdf = mvc.perform(get("/api/v1/report-histories/" + historyId + "/download").header("Authorization", admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(Arrays.copyOf(pdf, 4), StandardCharsets.US_ASCII)).isEqualTo("%PDF");
        // a user without history rights cannot fetch someone else's file
        mvc.perform(get("/api/v1/report-histories/" + historyId + "/download").header("Authorization", bearer("teller", "Teller123")))
                .andExpect(status().isNotFound());
        // bad parameter value -> 400, history marked FAILED instead of a 500
        mvc.perform(get("/api/v1/reports/export").header("Authorization", admin)
                        .param("id", String.valueOf(reportId)).param("format", "pdf").param("p_count", "abc"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"));

        // permission CRUD: viewer role runs it in the background
        JsonNode perm = body(mvc.perform(post("/api/v1/report-permissions").header("Authorization", admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":" + reportId + ",\"roleId\":" + viewerRoleId + ",\"runInBackground\":\"B\"}"))
                .andExpect(status().isCreated()));
        mvc.perform(post("/api/v1/report-permissions").header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportId\":" + reportId + ",\"roleId\":" + viewerRoleId + ",\"runInBackground\":\"V\"}"))
                .andExpect(status().isConflict());
        mvc.perform(get("/api/v1/report-permissions").param("reportId", String.valueOf(reportId)).header("Authorization", admin))
                .andExpect(jsonPath("$.content.length()").value(2));

        // viewer listens on its own queue, runs the report in the background and gets notified
        String viewerToken = login("viewer", "Viewer123").get("accessToken").asText();
        BlockingQueue<Map<String, Object>> inbox = new LinkedBlockingQueue<>();
        StompSession session = connect(viewerToken, inbox);
        BlockingQueue<Map<String, Object>> adminInbox = new LinkedBlockingQueue<>();
        StompSession adminSession = connect(login("admin", "Admin123").get("accessToken").asText(), adminInbox);
        Thread.sleep(500);

        JsonNode queued = body(mvc.perform(get("/api/v1/reports/export").header("Authorization", "Bearer " + viewerToken)
                        .param("id", String.valueOf(reportId)).param("format", "xlsx").param("p_count", "3"))
                .andExpect(status().isOk()));
        assertThat(queued.get("title").asText()).isEqualTo("Export Scheduled");

        Map<String, Object> note = inbox.poll(30, TimeUnit.SECONDS);
        assertThat(note).isNotNull().containsEntry("type", "success");
        assertThat(adminInbox.poll(1, TimeUnit.SECONDS)).isNull();

        String bgHistory = queued.at("/data/historyId").asText();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() ->
                mvc.perform(get("/api/v1/report-histories/" + bgHistory + "/download").header("Authorization", "Bearer " + viewerToken))
                        .andExpect(status().isOk()));

        mvc.perform(delete("/api/v1/report-permissions/" + perm.get("id").asText()).header("Authorization", admin))
                .andExpect(status().isNoContent());
        session.disconnect();
        adminSession.disconnect();
    }

    @Test
    void websocketRequiresAToken() {
        assertThatThrownBy(() -> connect(null, new LinkedBlockingQueue<>())).isInstanceOf(ExecutionException.class);
    }

    // ------------------------------------------------------------------ helpers

    private StompSession connect(String token, BlockingQueue<Map<String, Object>> inbox) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(
                new SockJsClient(List.of(new WebSocketTransport(new StandardWebSocketClient()))));
        client.setMessageConverter(new MappingJackson2MessageConverter());
        StompHeaders connect = new StompHeaders();
        if (token != null) {
            connect.add("Authorization", "Bearer " + token);
        }
        WebSocketHttpHeaders handshake = new WebSocketHttpHeaders();
        handshake.setOrigin("http://localhost:4200");
        StompSession session = client.connectAsync("http://localhost:" + port + "/ws", handshake, connect,
                new StompSessionHandlerAdapter() { }).get(5, TimeUnit.SECONDS);
        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                inbox.add((Map<String, Object>) payload);
            }
        });
        return session;
    }

    private JsonNode login(String username, String password) throws Exception {
        return body(attemptLogin(username, password).andExpect(status().isOk()));
    }

    private String bearer(String username, String password) throws Exception {
        return "Bearer " + login(username, password).get("accessToken").asText();
    }

    private ResultActions attemptLogin(String username, String password) throws Exception {
        return mvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("username", username, "password", password))));
    }

    private ResultActions refresh(String token) throws Exception {
        return mvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(Map.of("refreshToken", token))));
    }

    private JsonNode body(ResultActions result) throws Exception {
        return json.readTree(result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
