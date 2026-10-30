package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.LaboratoryRequestDTO;
import com.example.lab_manager.dto.response.LaboratoryResponseDTO;
import com.example.lab_manager.service.LaboratoryService;
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
@RequestMapping("/laboratories")
@RequiredArgsConstructor
@Tag(name = "Laboratórios", description = "Gestão e consulta de laboratórios")
public class LaboratoryController {

    private final LaboratoryService laboratoryService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Cadastra novo laboratório (ADMIN)")
    public ResponseEntity<LaboratoryResponseDTO> create(@Valid @RequestBody LaboratoryRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(laboratoryService.create(dto));
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista laboratórios ativos")
    public ResponseEntity<List<LaboratoryResponseDTO>> listActive() {
        return ResponseEntity.ok(laboratoryService.findAllActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Busca laboratório por ID")
    public ResponseEntity<LaboratoryResponseDTO> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(laboratoryService.findById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualiza laboratório (ADMIN)")
    public ResponseEntity<LaboratoryResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody LaboratoryRequestDTO dto) {
        return ResponseEntity.ok(laboratoryService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Desativa laboratório - Soft Delete (ADMIN)")
    public ResponseEntity<Void> deactivate(@PathVariable UUID id) {
        laboratoryService.deactivate(id);
        return ResponseEntity.noContent().build();
    }
}