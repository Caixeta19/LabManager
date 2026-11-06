package com.example.lab_manager.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReserveRequestDTO(
        @NotNull(message = "O ID do laboratório é obrigatório")
        UUID laboratoryId,

        @NotNull(message = "A data/hora de início é obrigatória")
        @Future(message = "A data de início deve ser no futuro")
        LocalDateTime dataHoraInicio,

        @NotNull(message = "A data/hora de fim é obrigatória")
        @Future(message = "A data de término deve ser no futuro")
        LocalDateTime dataHoraFim,

        @Size(max = 500, message = "O motivo deve ter no máximo 500 caracteres")
        String reason
) {
    public ReserveRequestDTO(UUID laboratoryId, LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim) {
        this(laboratoryId, dataHoraInicio, dataHoraFim, null);
    }
}
