package com.example.lab_manager.dto.response;

import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.model.Equipment;

import java.util.UUID;

public record EquipmentResponseDTO(
        UUID id, String name, String heritage, String description, EquipmentStatus status, UUID laboratoryId, String laboratoryName
) {
    public static EquipmentResponseDTO fromEntity(Equipment eq) {
        return new EquipmentResponseDTO(
                eq.getId(), eq.getName(), eq.getHeritage(), eq.getDescription(),
                eq.getStatus(), eq.getLaboratory().getId(), eq.getLaboratory().getName()
        );

}
    }