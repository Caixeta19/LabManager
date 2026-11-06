package com.example.lab_manager.dto.response;

import com.example.lab_manager.enums.LaboratoryStatus;

import java.util.List;
import java.util.UUID;

public record LaboratoryResponseDTO(
        UUID id,
        String name,
        String localization,
        Integer capacity,
        String description,
        Boolean active,
        LaboratoryStatus status,
        String openingHours,
        List<String> resources
) {}
