package com.example.lab_manager.dto.response;

import com.example.lab_manager.enums.ReserveStatus; // Enum de status da reserva
import com.example.lab_manager.enums.UserType;

import java.time.LocalDateTime;
import java.util.UUID;

/** RN27: nunca inclua tokenConfirmacao nem codigoQr aqui (ver ReserveResponseDTOTest). */
public record ReserveResponseDTO(
        UUID id,
        UUID laboratoryId,
        String laboratoryName,
        UUID userId,
        String userName,
        LocalDateTime dataHoraInicio,
        LocalDateTime dataHoraFim,
        ReserveStatus status,
        LocalDateTime dataConfirmacao,
        UserType userType,
        String reason,
        LocalDateTime createdAt
) {
    public ReserveResponseDTO(UUID id, UUID laboratoryId, String laboratoryName, UUID userId, String userName,
                              LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim, ReserveStatus status,
                              LocalDateTime dataConfirmacao) {
        this(id, laboratoryId, laboratoryName, userId, userName, dataHoraInicio, dataHoraFim, status,
                dataConfirmacao, null, null, null);
    }
}
