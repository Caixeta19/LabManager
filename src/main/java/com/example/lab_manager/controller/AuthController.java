package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.LoginRequestDTO;
import com.example.lab_manager.dto.response.AuthResponseDTO;
import com.example.lab_manager.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login e emissão do token JWT")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    @SecurityRequirements
    @Operation(summary = "Autentica o usuário e retorna o token JWT (público)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Login realizado"),
            @ApiResponse(responseCode = "401", description = "ERR_AUTH_001 - usuário ou senha inválidos"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - campos inválidos"),
            @ApiResponse(responseCode = "423", description = "ERR_AUTH_LOCKED - conta bloqueada por excesso de tentativas")
    })
    public ResponseEntity<AuthResponseDTO> login(@Valid @RequestBody LoginRequestDTO loginDTO) {
        return ResponseEntity.ok(authService.login(loginDTO));
    }
}
