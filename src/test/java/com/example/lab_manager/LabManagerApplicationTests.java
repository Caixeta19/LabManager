package com.example.lab_manager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integração: sobe o contexto inteiro contra um Postgres real (Testcontainers). Aplica V1+V2 pelo Flyway
 * e valida o mapeamento das entidades ({@code ddl-auto=validate}).
 * <p>
 * Fica fora do {@code ./mvnw test} padrão ({@code @Tag("integration")}). Rode com {@code ./mvnw test -Pintegration}.
 * Sem Docker disponível, o teste é pulado ({@code disabledWithoutDocker}).
 */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
		IntegrationTestProperties.JWT_SECRET,
		IntegrationTestProperties.BASE_URL,
		IntegrationTestProperties.MAIL_HOST,
		IntegrationTestProperties.MAIL_USERNAME,
		IntegrationTestProperties.MAIL_PASSWORD
})
class LabManagerApplicationTests {

	@Container
	@ServiceConnection
	static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

	@Test
	void contextLoads() {
	}

}
