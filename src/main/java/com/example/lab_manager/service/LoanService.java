package com.example.lab_manager.service;
import com.example.lab_manager.dto.request.LoanRequestDTO;
import com.example.lab_manager.dto.response.LoanResponseDTO;
import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.enums.LoanStatus;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.Equipment;
import com.example.lab_manager.model.Loan;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final UserService userService;
    private final EquipmentService equipmentService;

    @Transactional
    public LoanResponseDTO criar(LoanRequestDTO dto) {
        User user = userService.buscarEntidadePorId(dto.userId());
        Equipment equipment = equipmentService.buscarEntidadePorId(dto.equipmentId());

        equipmentService.alterarStatus(dto.equipmentId(), EquipmentStatus.EMPRESTADO);

        Loan loan = Loan.builder()
                .user(user)
                .equipment(equipment)
                .dataRetirada(LocalDateTime.now())
                .dataDevolucaoPrevista(dto.dataDevolucaoPrevista())
                .status(LoanStatus.EM_ANDAMENTO)
                .build();

        return LoanResponseDTO.fromEntity(loanRepository.save(loan));
    }

    @Transactional
    public LoanResponseDTO devolver(UUID id) {
        Loan loan = buscarEntidadePorId(id);

        if (loan.getStatus() == LoanStatus.DEVOLVIDO) {
            throw new RegraDeNegocioException("Este empréstimo já foi devolvido.");
        }

        loan.setDataDevolucaoReal(LocalDateTime.now());
        loan.setStatus(LoanStatus.DEVOLVIDO);

        equipmentService.alterarStatus(loan.getEquipment().getId(), EquipmentStatus.DISPONIVEL);

        return LoanResponseDTO.fromEntity(loanRepository.save(loan));
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> listarPorUsuario(UUID userId) {
        return loanRepository.findByUserId(userId).stream()
                .map(LoanResponseDTO::fromEntity)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> listarAtrasados() {
        return loanRepository.findByStatus(LoanStatus.ATRASADO).stream()
                .map(LoanResponseDTO::fromEntity)
                .toList();
    }

    Loan buscarEntidadePorId(UUID id) {
        return loanRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado: " + id));
    }
}

