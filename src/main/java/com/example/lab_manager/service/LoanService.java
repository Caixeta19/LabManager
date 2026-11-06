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
import com.example.lab_manager.repository.EquipmentRepository;
import com.example.lab_manager.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final EquipmentRepository equipmentRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    @Transactional
    public LoanResponseDTO createLoan(LoanRequestDTO dto) {
        // RN08: lock pessimista — o segundo pedido simultâneo espera e já vê EM_USO
        Equipment equipment = equipmentRepository.findByIdForUpdate(dto.equipmentId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado."));

        // RN07 / decisão 4
        if (equipment.getStatus() != EquipmentStatus.DISPONIVEL) {
            throw new RegraDeNegocioException("Este equipamento não está disponível no momento.",
                    "ERR_EQUIP_001", HttpStatus.CONFLICT);
        }

        User user = currentUserService.getUsuarioAutenticado();

        equipment.setStatus(EquipmentStatus.EM_USO);
        equipmentRepository.save(equipment);

        Loan loan = new Loan();
        loan.setEquipment(equipment);
        loan.setUser(user);
        loan.setDataRetirada(LocalDateTime.now(clock));
        loan.setDataDevolucaoPrevista(dto.dataDevolucaoPrevista());
        loan.setStatus(LoanStatus.ATIVO);

        return toDTO(loanRepository.save(loan));
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findAll() {
        return loanRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Empréstimos do usuário logado, do mais recente para o mais antigo. */
    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findMine() {
        User user = currentUserService.getUsuarioAutenticado();
        return loanRepository.findByUserIdOrderByDataRetiradaDesc(user.getId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** RN09: aceita empréstimos ATIVO ou ATRASADO. */
    @Transactional
    public LoanResponseDTO returnEquipment(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        if (loan.getStatus() != LoanStatus.ATIVO && loan.getStatus() != LoanStatus.ATRASADO) {
            throw new RegraDeNegocioException("Este empréstimo já foi encerrado com devolução.", "ERR_LOAN_001");
        }

        Equipment equipment = loan.getEquipment();
        equipment.setStatus(EquipmentStatus.DISPONIVEL);
        equipmentRepository.save(equipment);

        loan.setDataDevolucaoReal(LocalDateTime.now(clock));
        loan.setStatus(LoanStatus.DEVOLVIDO);

        return toDTO(loanRepository.save(loan));
    }

    private LoanResponseDTO toDTO(Loan loan) {
        return new LoanResponseDTO(
                loan.getId(),
                loan.getEquipment().getId(),
                loan.getEquipment().getName(),
                loan.getEquipment().getHeritage(),
                loan.getUser().getId(),
                loan.getUser().getName(),
                loan.getDataRetirada(),
                loan.getDataDevolucaoPrevista(),
                loan.getDataDevolucaoReal(),
                loan.getStatus()
        );
    }
}
