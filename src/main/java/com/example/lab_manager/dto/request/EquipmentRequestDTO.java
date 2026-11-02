package com.example.lab_manager.dto.request;

import com.example.lab_manager.enums.EquipmentStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EquipmentRequestDTO(
        @NotBlank(message = "O nome do equipamento é obrigatório")
        String name,

        @NotBlank(message = "O número de patrimônio (tombo) é obrigatório")
        String heritage,

        String description,

        @NotNull(message = "O ID do laboratório é obrigatório")
        UUID laboratoryId,

        /** Opcional. Ignorado no cadastro (RN06: nasce DISPONIVEL); no update aceita DISPONIVEL ou MANUTENCAO. */
        EquipmentStatus status
) {}
