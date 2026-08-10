package com.example.lab_manager.dto;

import com.example.lab_manager.model.CheckIn;

import java.time.LocalDateTime;
import java.util.UUID;

public record CheckInResponseDTO(
     UUID id, UUID reserveId, LocalDateTime horarioChegada
)
{
    public static CheckInResponseDTO fromEntity(CheckIn checkIn) {
        return new CheckInResponseDTO(
                checkIn.getId(), checkIn.getReserve().getId(), checkIn.getHorarioChegada()
        );
    }

}
