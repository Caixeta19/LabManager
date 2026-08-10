package com.example.lab_manager.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.UUID;

public record EquipmentRequestDTO(
        @NotBlank String name,
        @NotBlank String heritage,
        String description,
        @NotBlank UUID laboratoryId) {
}
