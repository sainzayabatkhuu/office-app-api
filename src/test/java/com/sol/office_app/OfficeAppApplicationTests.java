package com.sol.office_app;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Starts the whole application against a throw-away MySQL instead of the developer's database. */
@SpringBootTest(properties = {
		"security.jwt.secret-key=context-test-secret-key-0123456789abcdef",
		"spring.config.import="
})
@Testcontainers
class OfficeAppApplicationTests {

	@Container
	@ServiceConnection
	static MySQLContainer<?> mysql = new MySQLContainer<>("mysql:8.4");

	@Test
	void contextLoads() {
	}

}
