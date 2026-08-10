package com.example.lab_manager.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record LaboratoryRequestDTO(
        @NotBlank String name,
        @NotBlank String localization,
        @NotNull @Positive Integer capacity,
        String description) {
}
