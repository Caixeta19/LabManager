package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.ForgotPasswordRequestDTO;
import com.example.lab_manager.dto.request.LoginRequestDTO;
import com.example.lab_manager.dto.request.RegisterRequestDTO;
import com.example.lab_manager.dto.request.ResetPasswordRequestDTO;
import com.example.lab_manager.dto.response.AuthResponseDTO;
import com.example.lab_manager.dto.response.UserResponseDTO;
import com.example.lab_manager.service.AuthService;
import com.example.lab_manager.service.PasswordResetService;
import com.example.lab_manager.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping
@RequiredArgsConstructor
@Tag(name = "Autenticação", description = "Login e emissão do token JWT")
public class AuthController {

    private final AuthService authService;
    private final UserService userService;
    private final PasswordResetService passwordResetService;

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

    @PostMapping("/auth/register")
    @SecurityRequirements
    @Operation(summary = "Autocadastro; cria sempre um ALUNO, ignorando qualquer papel enviado (público)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Conta criada"),
            @ApiResponse(responseCode = "409", description = "ERR_USER_001 - matrícula já cadastrada; ERR_USER_002 - e-mail já cadastrado"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - campos inválidos")
    })
    public ResponseEntity<UserResponseDTO> register(@Valid @RequestBody RegisterRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.register(dto));
    }

    @PostMapping("/auth/forgot-password")
    @SecurityRequirements
    @Operation(summary = "Envia o link de redefinição de senha; a resposta não revela se o e-mail existe (público)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pedido recebido"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - e-mail em formato inválido")
    })
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequestDTO dto) {
        passwordResetService.solicitar(dto.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/auth/reset-password")
    @SecurityRequirements
    @Operation(summary = "Troca a senha usando o token recebido por e-mail (público)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Senha alterada"),
            @ApiResponse(responseCode = "400", description = "ERR_AUTH_002 - link inválido ou expirado"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - campos inválidos")
    })
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequestDTO dto) {
        passwordResetService.redefinir(dto);
        return ResponseEntity.noContent().build();
    }
}
