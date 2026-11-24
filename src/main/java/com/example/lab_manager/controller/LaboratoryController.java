package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.LaboratoryRequestDTO;
import com.example.lab_manager.dto.response.LaboratoryResponseDTO;
import com.example.lab_manager.dto.response.OccupiedSlotDTO;
import com.example.lab_manager.service.LaboratoryCheckinQrService;
import com.example.lab_manager.service.LaboratoryService;
import com.example.lab_manager.service.ReserveService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/laboratories")
@RequiredArgsConstructor
@Tag(name = "Laboratórios", description = "Gestão e consulta de laboratórios")
@ApiResponse(responseCode = "401", description = "Sem token ou token inválido/expirado")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;
    private final ReserveService reserveService;
    private final LaboratoryCheckinQrService checkinQrService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra novo laboratório (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Laboratório criado"),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - usuário não é ADMIN"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - campos inválidos")
    })
    public ResponseEntity<LaboratoryResponseDTO> create(@Valid @RequestBody LaboratoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratoryService.create(dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista laboratórios ativos")
    @ApiResponse(responseCode = "200", description = "Lista de laboratórios ativos")
    public ResponseEntity<List<LaboratoryResponseDTO>> listActive() {
        return ResponseEntity.ok(laboratoryService.findAllActive());
    }

    @GetMapping("/todos")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista todos os laboratórios, inclusive os inativos (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Lista de laboratórios"),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - usuário não é ADMIN")
    })
    public ResponseEntity<List<LaboratoryResponseDTO>> listAll() {
        return ResponseEntity.ok(laboratoryService.findAll());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca laboratório por ID")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Laboratório encontrado"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado")
    })
    public ResponseEntity<LaboratoryResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(laboratoryService.findById(id));
    }

    @GetMapping("/{id}/ocupacao")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Horários ocupados nos próximos 14 dias, sem identificar quem reservou")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Horários ocupados"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado")
    })
    public ResponseEntity<List<OccupiedSlotDTO>> occupiedSlots(@PathVariable UUID id) {
        return ResponseEntity.ok(reserveService.findOcupacao(id));
    }

    // Sem "produces": assim os erros (404, 403) continuam saindo em JSON pelo GlobalExceptionHandler
    @GetMapping("/{id}/checkin-qrcode")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Imagem PNG do QR fixo de check-in do laboratório, para imprimir na porta (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Imagem PNG do QR (URL do front com o token do laboratório)",
                    content = @Content(mediaType = MediaType.IMAGE_PNG_VALUE)),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - usuário não é ADMIN"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado"),
            @ApiResponse(responseCode = "500", description = "ERR_QRCODE_001 - falha ao gerar a imagem")
    })
    public ResponseEntity<byte[]> checkinQrCode(@PathVariable UUID id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_PNG).body(checkinQrService.gerarImagemPng(id));
    }

    @PostMapping("/{id}/checkin-token/regenerar")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Gera um novo token de check-in; o QR impresso antes deixa de valer (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Token trocado"),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - usuário não é ADMIN"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado")
    })
    public ResponseEntity<Void> regenerarCheckinToken(@PathVariable UUID id) {
        checkinQrService.regenerarToken(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza laboratório, inclusive o status ATIVO/INATIVO/MANUTENCAO (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Laboratório atualizado"),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - usuário não é ADMIN"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - campos inválidos")
    })
    public ResponseEntity<LaboratoryResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody LaboratoryRequestDTO dto) {
        return ResponseEntity.ok(laboratoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa laboratório - soft delete (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Laboratório desativado"),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - usuário não é ADMIN"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado")
    })
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        laboratoryService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}
