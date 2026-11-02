package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.ReserveRequestDTO;
import com.example.lab_manager.dto.response.ReserveResponseDTO;
import com.example.lab_manager.service.ReserveService;
import io.swagger.v3.oas.annotations.Operation;
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
    @Operation(summary = "Cria nova reserva com status PENDENTE")
    public ResponseEntity<ReserveResponseDTO> create(@Valid @RequestBody ReserveRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(reserveService.create(dto));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PROFESSOR')")
    @Operation(summary = "Lista todas as reservas do sistema (ADMIN, PROFESSOR)")
    public ResponseEntity<List<ReserveResponseDTO>> listAll() {
        return ResponseEntity.ok(reserveService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca reserva por ID (dono, ADMIN ou PROFESSOR; demais recebem 404)")
    public ResponseEntity<ReserveResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(reserveService.findById(id));
    }

    @PutMapping("/{id}/cancelar")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancela uma reserva (dono ou ADMIN; demais recebem 404)")
    public ResponseEntity<Void> cancel(@PathVariable UUID id) {
        reserveService.cancel(id);
        return ResponseEntity.noContent().build();
    }

    /** Rota antiga (README §9), mantida por compatibilidade. Mesma regra de {@code PUT /{id}/cancelar}. */
    @Deprecated
    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Cancela uma reserva (rota antiga; prefira PUT /reserves/{id}/cancelar)", deprecated = true)
    public ResponseEntity<Void> cancelLegacy(@PathVariable UUID id) {
        return cancel(id);
    }

    @GetMapping("/confirmar/{token}")
    @Operation(summary = "Confirma reserva via token de link único enviado por e-mail (Público)")
    public ResponseEntity<String> confirmByToken(@PathVariable String token) {
        reserveService.confirmByToken(token);
        return ResponseEntity.ok("Reserva confirmada com sucesso!");
    }

    // Sem "produces": assim os erros (404, 401) continuam saindo em JSON pelo GlobalExceptionHandler
    @GetMapping("/{id}/qrcode")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Gera e retorna a imagem PNG do QR Code da reserva sob demanda (só o dono)")
    public ResponseEntity<byte[]> getQrCodeImage(@PathVariable UUID id) {
        byte[] qrCodePng = reserveService.generateQrCodeImage(id);
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(qrCodePng);
    }
}