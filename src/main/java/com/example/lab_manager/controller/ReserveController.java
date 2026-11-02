package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.ReserveRequestDTO;
import com.example.lab_manager.dto.response.ReserveResponseDTO;
import com.example.lab_manager.service.ReserveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/reserves")
@RequiredArgsConstructor
@Tag(name = "Reservas", description = "Ciclo de vida e gestão de reservas de laboratório")
public class ReserveController {

    private final ReserveService reserveService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ALUNO', 'PROFESSOR')")
    @Operation(summary = "Cria nova reserva com status PENDENTE (ALUNO, PROFESSOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva criada (sem token nem código do QR no corpo)"),
            @ApiResponse(responseCode = "400", description = "Fim antes do início; ERR_LAB_001 - laboratório inativo"),
            @ApiResponse(responseCode = "401", description = "Sem token ou token inválido/expirado"),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - papel sem permissão"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado"),
            @ApiResponse(responseCode = "409", description = "ERR_RESERVA_001 - conflito de horário"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - campos inválidos")
    })
    public ResponseEntity<ReserveResponseDTO> create(@Valid @RequestBody ReserveRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reserveService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(summary = "Lista todas as reservas do sistema (ADMIN, PROFESSOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de reservas"),
            @ApiResponse(responseCode = "401", description = "Sem token ou token inválido/expirado"),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - papel sem permissão")
    })
    public ResponseEntity<List<ReserveResponseDTO>> listAll() {
        return ResponseEntity.ok(reserveService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca reserva por ID (dono, ADMIN ou PROFESSOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva encontrada"),
            @ApiResponse(responseCode = "401", description = "Sem token ou token inválido/expirado"),
            @ApiResponse(responseCode = "404", description = "Reserva inexistente ou sem acesso")
    })
    public ResponseEntity<ReserveResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(reserveService.findById(id));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancela uma reserva (dono ou ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva cancelada"),
            @ApiResponse(responseCode = "400", description = "ERR_RESERVA_003 - reserva já finalizada ou cancelada"),
            @ApiResponse(responseCode = "401", description = "Sem token ou token inválido/expirado"),
            @ApiResponse(responseCode = "404", description = "Reserva inexistente ou sem acesso")
    })
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        reserveService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    /** Rota antiga (README §9), mantida por compatibilidade. Mesma regra de {@code PUT /{id}/cancelar}. */
    @Deprecated
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancela uma reserva (rota antiga; prefira PUT /reserves/{id}/cancelar)", deprecated = true)
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva cancelada"),
            @ApiResponse(responseCode = "400", description = "ERR_RESERVA_003 - reserva já finalizada ou cancelada"),
            @ApiResponse(responseCode = "404", description = "Reserva inexistente ou sem acesso")
    })
    public ResponseEntity<Void> cancelLegacy(@PathVariable UUID id) {
        return cancel(id);
    }

    @GetMapping("/confirmar/{token}")
    @SecurityRequirements
    @Operation(summary = "Confirma a reserva pelo link único enviado por e-mail (público)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva confirmada"),
            @ApiResponse(responseCode = "404", description = "Token de confirmação inválido"),
            @ApiResponse(responseCode = "409", description = "ERR_RESERVA_002 - reserva não pode mais ser confirmada")
    })
    public ResponseEntity<String> confirmByToken(@PathVariable String token) {
        reserveService.confirmByToken(token);
        return ResponseEntity.ok("Reserva confirmada com sucesso!");
    }

    // Sem "produces": assim os erros (404, 401) continuam saindo em JSON pelo GlobalExceptionHandler
    @GetMapping("/{id}/qrcode")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gera a imagem PNG do QR Code da reserva sob demanda (só o dono)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem PNG do QR Code",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "401", description = "Sem token ou token inválido/expirado"),
            @ApiResponse(responseCode = "404", description = "Reserva inexistente ou de outro usuário"),
            @ApiResponse(responseCode = "500", description = "ERR_QRCODE_001 - falha ao gerar a imagem")
    })
    public ResponseEntity<byte[]> getQrCodeImage(@PathVariable UUID id) {
        byte[] qrCodePng = reserveService.generateQrCodeImage(id);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrCodePng);
    }
}
