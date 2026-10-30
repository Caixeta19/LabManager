package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.LoanRequestDTO;
import com.example.lab_manager.dto.response.LoanResponseDTO;
import com.example.lab_manager.service.LoanService;
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
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Empréstimos", description = "Solicitação e controle de devolução de equipamentos")
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ALUNO', 'PROFESSOR')")
    @Operation(summary = "Solicita empréstimo de equipamento disponível")
    public ResponseEntity<LoanResponseDTO> create(@Valid @RequestBody LoanRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.createLoan(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista todos os empréstimos (ADMIN)")
    public ResponseEntity<List<LoanResponseDTO>> listAll() {
        return ResponseEntity.ok(loanService.findAll());
    }

    @PutMapping("/{id}/devolver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra a devolução do equipamento e reverte status para DISPONÍVEL (ADMIN)")
    public ResponseEntity<LoanResponseDTO> returnEquipment(@PathVariable UUID id) {
        return ResponseEntity.ok(loanService.returnEquipment(id));
    }
}