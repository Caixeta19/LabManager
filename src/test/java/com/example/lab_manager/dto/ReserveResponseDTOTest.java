package com.example.lab_manager.dto;

import com.example.lab_manager.dto.response.ReserveResponseDTO;
import com.example.lab_manager.enums.ReserveStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;

import java.lang.reflect.RecordComponent;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Regressão de segurança (RN27 / ADR-02): tokenConfirmacao e codigoQr nunca aparecem no DTO de reserva.
 * Se este teste quebrar, alguém expôs dados que permitem confirmar ou fazer check-in no lugar do dono.
 */
class ReserveResponseDTOTest {

    @Test
    void recordNaoTemCamposSensiveis() {
        assertThat(Arrays.stream(ReserveResponseDTO.class.getRecordComponents()).map(RecordComponent::getName))
                .doesNotContain("tokenConfirmacao", "codigoQr")
                .noneMatch(nome -> nome.toLowerCase().contains("token") || nome.toLowerCase().contains("qr"));
    }

    @Test
    void jsonSerializadoNaoTemCamposSensiveis() throws Exception {
        var dto = new ReserveResponseDTO(UUID.randomUUID(), UUID.randomUUID(), "Lab", UUID.randomUUID(), "Fulano",
                LocalDateTime.now(), LocalDateTime.now().plusHours(1), ReserveStatus.PENDENTE, null);

        String json = new ObjectMapper().registerModule(new JavaTimeModule()).writeValueAsString(dto);

        assertThat(json).doesNotContainIgnoringCase("token").doesNotContainIgnoringCase("qr");
    }
}
