package com.example.lab_manager.dto;

import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.model.Reserve;

import java.time.LocalDateTime;
import java.util.UUID;

public record ReserveResponseDTO(
        UUID id, UUID userId, String userName, UUID laboratoryId, String laboratoryName, LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim, ReserveStatus status, boolean emailConfirmacaoEnviado
) {
    public static ReserveResponseDTO fromEntity(Reserve reserve) {
        return new ReserveResponseDTO(
                reserve.getId(), reserve.getUser().getId(), reserve.getUser().getName(),
                reserve.getLaboratory().getId(), reserve.getLaboratory().getName(),
                reserve.getDataHorainicio(), reserve.getDataHoraFim(),
                reserve.getStatus(), reserve.isEmailConfirmacaoEnviado()

        );
    }
}
