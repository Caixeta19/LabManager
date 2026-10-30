package com.example.lab_manager.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record EquipmentRequestDTO(
        @NotBlank(message = "O nome do equipamento é obrigatório")
        String name,

        @NotBlank(message = "O número de patrimônio (tombo) é obrigatório")
        String heritage,

        String description,

        @NotNull(message = "O ID do laboratório é obrigatório")
        Long laboratoryId
) {}