package com.example.lab_manager.dto.response;

import com.example.lab_manager.enums.ReserveStatus; // Enum de status da reserva
import java.time.LocalDateTime;
import java.util.UUID;

public record ReserveResponseDTO(
        UUID id,
        UUID laboratoryId,
        String laboratoryName,
        UUID userId,
        String userName,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        ReserveStatus status,
        LocalDateTime dataConfirmacao
) {}