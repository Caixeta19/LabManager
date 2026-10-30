package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.EquipmentRequestDTO;
import com.example.lab_manager.dto.response.EquipmentResponseDTO;
import com.example.lab_manager.service.EquipmentService;
import io.swagger.v3.oas.annotations.Operation;
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
public class EquipmentController {

    private final EquipmentService equipmentService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra novo equipamento com status DISPONÍVEL (ADMIN)")
    public ResponseEntity<EquipmentResponseDTO> create(@Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(equipmentService.create(dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista todos os equipamentos")
    public ResponseEntity<List<EquipmentResponseDTO>> listAll() {
        return ResponseEntity.ok(equipmentService.findAll());
    }

    @GetMapping("/disponiveis")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista apenas equipamentos com status DISPONÍVEL")
    public ResponseEntity<List<EquipmentResponseDTO>> listAvailable() {
        return ResponseEntity.ok(equipmentService.findAllAvailable());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza dados ou status de equipamento (ADMIN)")
    public ResponseEntity<EquipmentResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody EquipmentRequestDTO dto) {
        return ResponseEntity.ok(equipmentService.update(id, dto));
    }
}