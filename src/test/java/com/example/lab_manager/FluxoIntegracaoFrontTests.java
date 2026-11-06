package com.example.lab_manager;

import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** O caminho que o front percorre, de ponta a ponta, contra um Postgres real. */
@Tag("integration")
@Testcontainers(disabledWithoutDocker = true)
@AutoConfigureMockMvc
@SpringBootTest(properties = {
        IntegrationTestProperties.JWT_SECRET,
        IntegrationTestProperties.BASE_URL,
        IntegrationTestProperties.FRONT_URL,
        IntegrationTestProperties.MAIL_HOST,
        IntegrationTestProperties.MAIL_USERNAME,
        IntegrationTestProperties.MAIL_PASSWORD
})
class FluxoIntegracaoFrontTests {

    private static final String ORIGEM_DO_FRONT = "http://localhost:5173";

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @MockBean JavaMailSender mailSender;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired LaboratoryRepository laboratoryRepository;

    @Test
    void cadastroLoginReservaEResumo() throws Exception {
        Laboratory lab = laboratoryRepository.save(Laboratory.builder()
                .name("Lab. de Redes").localization("Bloco A").capacity(30).build());

        // register: o papel enviado é ignorado
        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "name", "Ana Silva", "email", "ANA@lab.com", "password", "Senha@123",
                        "registration", "2026001", "type", "ADMIN"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type", is("ALUNO")))
                .andExpect(jsonPath("$.email", is("ana@lab.com")));

        // login
        JsonNode login = json.readTree(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "ana@lab.com", "senha", "Senha@123"))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString());
        String bearer = "Bearer " + login.get("token").asText();

        // /auth/me
        mvc.perform(get("/auth/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Ana Silva")))
                .andExpect(jsonPath("$.lastLoginAt", notNullValue()));

        // reserva
        LocalDateTime inicio = LocalDateTime.now().plusMinutes(1).truncatedTo(ChronoUnit.SECONDS);
        DateTimeFormatter formato = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
        String corpo = mvc.perform(post("/reserves").header(HttpHeaders.AUTHORIZATION, bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of(
                                "laboratoryId", lab.getId().toString(),
                                "dataHoraInicio", inicio.format(formato),
                                "dataHoraFim", inicio.plusHours(1).format(formato),
                                "reason", "Aula prática de redes"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status", is("PENDENTE")))
                .andExpect(jsonPath("$.userType", is("ALUNO")))
                .andExpect(jsonPath("$.reason", is("Aula prática de redes")))
                .andReturn().getResponse().getContentAsString();
        assertThat(corpo).doesNotContainIgnoringCase("token").doesNotContainIgnoringCase("codigoQr");

        // /reserves/me
        mvc.perform(get("/reserves/me").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].laboratoryName", is("Lab. de Redes")))
                .andExpect(jsonPath("$[0].createdAt", notNullValue()));

        // /public/resumo, sem token
        int reservasHoje = inicio.toLocalDate().equals(LocalDate.now()) ? 1 : 0;
        mvc.perform(get("/public/resumo"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.laboratoriosAtivos", is(1)))
                .andExpect(jsonPath("$.reservasHoje", is(reservasHoje)))
                .andExpect(jsonPath("$.usuarios", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.laboratorios[0].nome", is("Lab. de Redes")))
                .andExpect(jsonPath("$.laboratorios[0].status", is("DISPONIVEL")));
    }

    @Test
    void preflightDoNavegadorPassaSemToken() throws Exception {
        mvc.perform(options("/reserves/me")
                        .header(HttpHeaders.ORIGIN, ORIGEM_DO_FRONT)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, ORIGEM_DO_FRONT));
    }

    @Test
    void origemDesconhecidaERecusada() throws Exception {
        mvc.perform(options("/reserves/me")
                        .header(HttpHeaders.ORIGIN, "http://site-malicioso.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rotasNovasExigemTokenEPapel() throws Exception {
        mvc.perform(get("/auth/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/reserves/me")).andExpect(status().isUnauthorized());
        mvc.perform(get("/loans/me")).andExpect(status().isUnauthorized());

        mvc.perform(post("/auth/register").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of(
                        "name", "Bruno Costa", "email", "bruno@lab.com", "password", "Senha@123",
                        "registration", "2026002"))))
                .andExpect(status().isCreated());
        String token = json.readTree(mvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "bruno@lab.com", "senha", "Senha@123"))))
                .andReturn().getResponse().getContentAsString()).get("token").asText();

        mvc.perform(get("/laboratories/todos").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isForbidden());
        mvc.perform(put("/users/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("name", "X", "email", "x@lab.com",
                                "registration", "1", "type", "ADMIN", "active", true))))
                .andExpect(status().isForbidden());
    }

    @Test
    void esqueciMinhaSenhaRespondeSempre204() throws Exception {
        mvc.perform(post("/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(Map.of("email", "ninguem@lab.com"))))
                .andExpect(status().isNoContent());
    }
}
