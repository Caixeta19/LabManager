package com.example.lab_manager.dto.response;

import com.example.lab_manager.enums.EquipmentStatus;

import java.util.UUID;

public record EquipmentResponseDTO(
        UUID id,
        String name,
        String heritage,
        String description,
        UUID laboratoryId,
        String laboratoryName,
        EquipmentStatus status,
        String responsible
) {}
