package com.example.lab_manager.dto.response;

import java.util.UUID;

public record LaboratoryResponseDTO(
        UUID id,
        String name,
        String localization,
        Integer capacity,
        String description,
        Boolean active
) {}