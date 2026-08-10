package com.example.lab_manager.dto.response;

import com.example.lab_manager.model.Laboratory;

import java.util.UUID;

public record LaboratoryResponseDTO(
        UUID id, String name, String localization, Integer capacity, String description, boolean active
) {
    public static LaboratoryResponseDTO fromEntity(Laboratory lab){
        return new LaboratoryResponseDTO(
                lab.getId(), lab.getName(),lab.getLocatization(), lab.getCapacity(), lab.getDescription(), lab.getActive()
        );
    }
}
