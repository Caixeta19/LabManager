package com.example.lab_manager.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@Tag(name = "Saúde", description = "Status da aplicação")
public class HealthController {

    @GetMapping("/health")
    @SecurityRequirements
    @Operation(summary = "Status da aplicação (público)")
    @ApiResponse(responseCode = "200", description = "Aplicação no ar: {\"status\":\"ok\"}")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}
