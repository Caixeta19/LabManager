package com.example.lab_manager.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReserveRequestDTO(
        @NotNull UUID userId,
        @NotNull UUID laboratoryId,
        @NotNull @Future LocalDateTime dataHoraInicio,
        @NotNull @Future LocalDateTime dataHoraFim
        ) {
}
