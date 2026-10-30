package com.example.lab_manager.dto.response;

import com.example.lab_manager.enums.LoanStatus; // Enum de status do empréstimo
import java.time.LocalDate;
import java.util.UUID;

public record LoanResponseDTO(
        UUID id,
        UUID equipmentId,
        String equipmentName,
        String heritage,
        UUID userId,
        String userName,
        LocalDate dataRetirada,
        LocalDate dataDevolucaoPrevista,
        LocalDate dataDevolucaoReal,
        LoanStatus status
) {}