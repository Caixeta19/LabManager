package com.example.lab_manager.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record CheckInResponseDTO(
        UUID id,
        UUID reserveId,
        LocalDateTime dataHoraCheckIn,
        String message
) {}