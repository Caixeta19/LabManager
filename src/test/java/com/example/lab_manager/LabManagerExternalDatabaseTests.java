package com.example.lab_manager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Mesma verificação do {@link LabManagerApplicationTests}, mas contra um Postgres já existente,
 * para máquinas sem Docker. Só roda se {@code IT_DB_URL} estiver definida. Use um banco
 * <b>descartável</b>: o Flyway aplica as migrations nele.
 * <pre>
 * IT_DB_URL=jdbc:postgresql://localhost:5432/labmanager_it IT_DB_USERNAME=labmanager IT_DB_PASSWORD=labmanager \
 *   ./mvnw test -Pintegration
 * </pre>
 */
@Tag("integration")
@EnabledIfEnvironmentVariable(named = "IT_DB_URL", matches = ".+")
@SpringBootTest(properties = {
        "spring.datasource.url=${IT_DB_URL}",
        "spring.datasource.username=${IT_DB_USERNAME:labmanager}",
        "spring.datasource.password=${IT_DB_PASSWORD:labmanager}",
        IntegrationTestProperties.JWT_SECRET,
        IntegrationTestProperties.BASE_URL,
        IntegrationTestProperties.MAIL_HOST,
        IntegrationTestProperties.MAIL_USERNAME,
        IntegrationTestProperties.MAIL_PASSWORD
})
class LabManagerExternalDatabaseTests {

    @Test
    void contextLoadsComMigrationsAplicadas() {
    }
}
