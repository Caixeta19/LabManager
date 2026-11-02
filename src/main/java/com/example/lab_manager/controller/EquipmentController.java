package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.EquipmentRequestDTO;
import com.example.lab_manager.dto.response.EquipmentResponseDTO;
import com.example.lab_manager.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/equipments")
@RequiredArgsConstructor
@Tag(name = "Equipamentos", description = "Gestão e consulta patrimonial de equipamentos")
@ApiResponse(responseCode = "401", description = "Sem token ou token inválido/expirado")
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra novo equipamento com status DISPONIVEL (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Equipamento criado"),
            @ApiResponse(responseCode = "400", description = "ERR_LAB_001 - laboratório inativo"),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - usuário não é ADMIN"),
            @ApiResponse(responseCode = "404", description = "Laboratório não encontrado"),
            @ApiResponse(responseCode = "409", description = "ERR_EQUIP_002 - patrimônio já cadastrado"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - campos inválidos")
    })
    public ResponseEntity<EquipmentResponseDTO> create(@Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista todos os equipamentos")
    @ApiResponse(responseCode = "200", description = "Lista de equipamentos")
    public ResponseEntity<List<EquipmentResponseDTO>> listAll() {
        return ResponseEntity.ok(equipmentService.findAll());
    }

    @GetMapping("/disponiveis")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista apenas equipamentos com status DISPONIVEL")
    @ApiResponse(responseCode = "200", description = "Lista de equipamentos disponíveis")
    public ResponseEntity<List<EquipmentResponseDTO>> listAvailable() {
        return ResponseEntity.ok(equipmentService.findAllAvailable());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza dados, laboratório ou status (DISPONIVEL/MANUTENCAO) do equipamento (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Equipamento atualizado"),
            @ApiResponse(responseCode = "400", description = "ERR_LAB_001 - laboratório inativo; ERR_EQUIP_003 - EM_USO só via empréstimo"),
            @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - usuário não é ADMIN"),
            @ApiResponse(responseCode = "404", description = "Equipamento ou laboratório não encontrado"),
            @ApiResponse(responseCode = "409", description = "ERR_EQUIP_002 - patrimônio já cadastrado"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - campos inválidos")
    })
    public ResponseEntity<EquipmentResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.ok(equipmentService.update(id, dto));
    }
}
