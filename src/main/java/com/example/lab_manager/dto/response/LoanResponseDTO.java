package com.example.lab_manager.dto.response;

import com.example.lab_manager.enums.LoanStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record LoanResponseDTO(
        UUID id,
        UUID equipmentId,
        String equipmentName,
        String heritage,
        UUID userId,
        String userName,
        LocalDateTime dataRetirada,
        LocalDate dataDevolucaoPrevista,
        LocalDateTime dataDevolucaoReal,
        LoanStatus status
) {}
