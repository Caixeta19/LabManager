package com.example.lab_manager.dto;

import com.example.lab_manager.enums.LoanStatus;
import com.example.lab_manager.model.Loan;

import java.time.LocalDateTime;
import java.util.UUID;

public record LoanResponseDTO(
        UUID id, String userName, String equipmentName, LocalDateTime dataRetirada, LocalDateTime dataDevolucaoPrevista, LocalDateTime dataDevolucaoReal, LoanStatus status
) {
    public static LoanResponseDTO fromEntity(Loan loan) {
        return new LoanResponseDTO(
                loan.getId(), loan.getUser().getName(), loan.getEquipment().getName(),
                loan.getDataRetirada(), loan.getDataDevolucaoPrevista(),
                loan.getDataDevolucaoReal(), loan.getStatus()

        );
    }
}
