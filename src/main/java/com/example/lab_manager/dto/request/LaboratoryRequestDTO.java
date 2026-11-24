package com.example.lab_manager.dto.request;

import com.example.lab_manager.enums.LaboratoryStatus;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

public record LaboratoryRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @NotBlank(message = "A localização é obrigatória")
        String localization,

        @NotNull(message = "A capacidade é obrigatória")
        @Positive(message = "A capacidade deve ser maior que zero")
        Integer capacity,

        String description,

        @Size(max = 120, message = "O horário de funcionamento deve ter no máximo 120 caracteres")
        String openingHours,

        List<String> resources,

        /** Opcional. Sem valor, o cadastro nasce ATIVO e o update mantém o status atual. */
        LaboratoryStatus status,

        /** Opcional ("HH:mm"). Informe os dois ou nenhum; abertura antes do fechamento. */
        @JsonFormat(pattern = "HH:mm")
        LocalTime bookingOpenTime,

        @JsonFormat(pattern = "HH:mm")
        LocalTime bookingCloseTime,

        /** Opcional. Dias de funcionamento ("MONDAY" … "SUNDAY"). Vazio ou ausente = qualquer dia. */
        List<DayOfWeek> bookingDays
) {}
