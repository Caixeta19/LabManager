package com.example.lab_manager.dto;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoanRequestDTO(
        @NotNull UUID userId,
        @NotNull UUID equipmentId,
        @NotNull @Future LocalDateTime dataDevolucaoPrevista
        ) {
}
