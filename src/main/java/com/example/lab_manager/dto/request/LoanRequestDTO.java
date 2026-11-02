package com.example.lab_manager.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.UUID;

public record LoanRequestDTO(
        @NotNull(message = "O ID do equipamento é obrigatório")
        UUID equipmentId,

        @NotNull(message = "A data de devolução prevista é obrigatória")
        @Future(message = "A data de devolução prevista deve ser uma data futura")
        LocalDate dataDevolucaoPrevista
) {}