package com.example.lab_manager.dto.response;

import com.example.lab_manager.enums.PublicLaboratoryStatus;

import java.util.List;

/** Resposta pública, sem autenticação: só agregados, nunca dados pessoais. */
public record PublicSummaryDTO(
        long laboratoriosAtivos,
        long reservasHoje,
        long equipamentos,
        long usuarios,
        List<LaboratorioDTO> laboratorios
) {
    public record LaboratorioDTO(String nome, PublicLaboratoryStatus status) {}
}
