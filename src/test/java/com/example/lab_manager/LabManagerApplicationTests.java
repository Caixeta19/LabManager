package com.example.lab_manager;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Teste de integração: sobe o contexto completo e precisa de banco.
 * Fica fora da execução padrão de {@code ./mvnw test}; rode com {@code ./mvnw verify -Pintegration}.
 */
@Tag("integration")
@SpringBootTest
class LabManagerApplicationTests {

	@Test
	void contextLoads() {
	}

}
