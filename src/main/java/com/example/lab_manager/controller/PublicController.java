package com.example.lab_manager.controller;

import com.example.lab_manager.dto.response.PublicSummaryDTO;
import com.example.lab_manager.service.PublicSummaryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/public")
@RequiredArgsConstructor
@Tag(name = "Público", description = "Dados agregados exibidos antes do login")
public class PublicController {

    private final PublicSummaryService publicSummaryService;

    @GetMapping("/resumo")
    @SecurityRequirements
    @Operation(summary = "Totais do sistema e situação dos laboratórios, sem dados pessoais (público)")
    @ApiResponse(responseCode = "200", description = "Resumo do sistema")
    public PublicSummaryDTO resumo() {
        return publicSummaryService.resumo();
    }
}
