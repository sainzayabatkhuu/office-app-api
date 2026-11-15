package com.sol.office.notification;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.sol.office.common.event.MessagingConstants;
import com.sol.office.common.event.NotificationEvent;
import com.sol.office.notification.dto.NotificationDtos.NotificationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.lang.reflect.Type;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.awaitility.Awaitility.await;

/**
 * Event on RabbitMQ -> stored -> pushed over STOMP to the right user only -> visible through REST.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class NotificationFlowIT {

    @Container
    @ServiceConnection
    static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

    @Container
    @ServiceConnection
    static RabbitMQContainer rabbit = new RabbitMQContainer("rabbitmq:4.1-management");

    /** Stands in for identity-service: same RS256 tokens, local key. */
    @TestConfiguration
    static class TokenConfig {
        static final RSAKey KEY = generate();

        @Bean
        JwtDecoder jwtDecoder() throws Exception {
            return NimbusJwtDecoder.withPublicKey(KEY.toRSAPublicKey()).build();
        }

        @Bean
        JwtEncoder testJwtEncoder() {
            return new NimbusJwtEncoder(new ImmutableJWKSet<>(new JWKSet(KEY)));
        }

        private static RSAKey generate() {
            try {
                KeyPairGenerator g = KeyPairGenerator.getInstance("RSA");
                g.initialize(2048);
                var pair = g.generateKeyPair();
                return new RSAKey.Builder((RSAPublicKey) pair.getPublic()).privateKey((RSAPrivateKey) pair.getPrivate()).keyID("test").build();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }
    }

    @LocalServerPort
    int port;
    @Autowired
    RabbitTemplate rabbitTemplate;
    @Autowired
    TestRestTemplate rest;
    @Autowired
    JwtEncoder testJwtEncoder;
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void eventIsStoredAndPushedToTheRecipientOnly() throws Exception {
        BlockingQueue<NotificationResponse> batInbox = new LinkedBlockingQueue<>();
        BlockingQueue<NotificationResponse> dorjInbox = new LinkedBlockingQueue<>();
        StompSession bat = connect(token("bat"), batInbox);
        StompSession dorj = connect(token("dorj"), dorjInbox);
        Thread.sleep(500); // let SUBSCRIBE frames reach the broker

        rabbitTemplate.convertAndSend(MessagingConstants.NOTIFICATION_EXCHANGE, MessagingConstants.NOTIFICATION_ROUTING_KEY,
                NotificationEvent.of("report-service", "bat", "SUCCESS", "REPORT", "Тайлан бэлэн боллоо",
                        "Ажилтны жагсаалт тайлан амжилттай үүслээ.", "/report-history", Map.of("executionId", "abc")));

        NotificationResponse pushed = batInbox.poll(15, TimeUnit.SECONDS);
        assertThat(pushed).isNotNull();
        assertThat(pushed.title()).isEqualTo("Тайлан бэлэн боллоо");
        assertThat(pushed.data()).containsEntry("executionId", "abc");
        assertThat(dorjInbox.poll(1, TimeUnit.SECONDS)).isNull();

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token("bat"));
        await().untilAsserted(() -> {
            var count = rest.exchange("/api/v1/notifications/unread-count", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            assertThat(count.getBody()).containsEntry("count", 1);
        });
        rest.exchange("/api/v1/notifications/" + pushed.id() + "/read", HttpMethod.POST, new HttpEntity<>(headers), Map.class);
        var after = rest.exchange("/api/v1/notifications/unread-count", HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        assertThat(after.getBody()).containsEntry("count", 0);

        // dorj cannot mark bat's notification
        HttpHeaders dorjHeaders = new HttpHeaders();
        dorjHeaders.setBearerAuth(token("dorj"));
        var forbidden = rest.exchange("/api/v1/notifications/" + pushed.id() + "/read", HttpMethod.POST, new HttpEntity<>(dorjHeaders), Map.class);
        assertThat(forbidden.getStatusCode().value()).isEqualTo(404);

        bat.disconnect();
        dorj.disconnect();
    }

    @Test
    void connectWithoutTokenIsRejected() {
        assertThatThrownBy(() -> connect(null, new LinkedBlockingQueue<>()))
                .isInstanceOf(ExecutionException.class);
    }

    private StompSession connect(String token, BlockingQueue<NotificationResponse> inbox) throws Exception {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(objectMapper);
        client.setMessageConverter(converter);
        StompHeaders connectHeaders = new StompHeaders();
        if (token != null) {
            connectHeaders.add("Authorization", "Bearer " + token);
        }
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(),
                connectHeaders, new StompSessionHandlerAdapter() {
                }).get(5, TimeUnit.SECONDS);
        session.subscribe("/user/queue/notifications", new StompFrameHandler() {
            @Override
            @NonNull
            public Type getPayloadType(@NonNull StompHeaders headers) {
                return NotificationResponse.class;
            }

            @Override
            public void handleFrame(@NonNull StompHeaders headers, Object payload) {
                inbox.add((NotificationResponse) payload);
            }
        });
        return session;
    }

    private String token(String username) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder().subject(username).issuedAt(now).expiresAt(now.plusSeconds(600))
                .claim("roles", List.of("REPORT_USER")).claim("authorities", List.of("REPORT_VIEW")).build();
        return testJwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), claims)).getTokenValue();
    }
}
