package com.example.lab_manager.controller;

import com.example.lab_manager.dto.request.LoanRequestDTO;
import com.example.lab_manager.dto.response.LoanResponseDTO;
import com.example.lab_manager.service.LoanService;
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
@RequestMapping("/loans")
@RequiredArgsConstructor
@Tag(name = "Empréstimos", description = "Solicitação e controle de devolução de equipamentos")
@ApiResponses({
        @ApiResponse(responseCode = "401", description = "Sem token ou token inválido/expirado"),
        @ApiResponse(responseCode = "403", description = "ERR_FORBIDDEN - papel sem permissão")
})
public class LoanController {

    private final LoanService loanService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ALUNO', 'PROFESSOR')")
    @Operation(summary = "Solicita empréstimo de equipamento disponível (ALUNO, PROFESSOR)")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Empréstimo criado; equipamento passa para EM_USO"),
            @ApiResponse(responseCode = "404", description = "Equipamento não encontrado"),
            @ApiResponse(responseCode = "409", description = "ERR_EQUIP_001 - equipamento indisponível"),
            @ApiResponse(responseCode = "422", description = "ERR_VALIDACAO - campos inválidos")
    })
    public ResponseEntity<LoanResponseDTO> create(@Valid @RequestBody LoanRequestDTO dto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(loanService.createLoan(dto));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lista todos os empréstimos (ADMIN)")
    @ApiResponse(responseCode = "200", description = "Lista de empréstimos")
    public ResponseEntity<List<LoanResponseDTO>> listAll() {
        return ResponseEntity.ok(loanService.findAll());
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Lista os empréstimos do usuário logado, do mais recente para o mais antigo")
    @ApiResponse(responseCode = "200", description = "Lista de empréstimos")
    public ResponseEntity<List<LoanResponseDTO>> listMine() {
        return ResponseEntity.ok(loanService.findMine());
    }

    @PutMapping("/{id}/devolver")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Registra a devolução e volta o equipamento para DISPONIVEL (ADMIN)")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Devolução registrada"),
            @ApiResponse(responseCode = "400", description = "ERR_LOAN_001 - empréstimo já devolvido"),
            @ApiResponse(responseCode = "404", description = "Empréstimo não encontrado")
    })
    public ResponseEntity<LoanResponseDTO> returnEquipment(@PathVariable UUID id) {
        return ResponseEntity.ok(loanService.returnEquipment(id));
    }
}
