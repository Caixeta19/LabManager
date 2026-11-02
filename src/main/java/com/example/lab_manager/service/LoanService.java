package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.LoanRequestDTO;
import com.example.lab_manager.dto.response.LoanResponseDTO;
import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.enums.LoanStatus;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.*;
import com.example.lab_manager.repository.EquipmentRepository;
import com.example.lab_manager.repository.LoanRepository;
import com.example.lab_manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LoanService {

    private final LoanRepository loanRepository;
    private final EquipmentRepository equipmentRepository;
    private final UserRepository userRepository;

    @Transactional
    public LoanResponseDTO createLoan(LoanRequestDTO dto) {
        Equipment equipment = equipmentRepository.findById(dto.equipmentId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Equipamento não encontrado."));

        if (equipment.getStatus() != EquipmentStatus.DISPONIVEL) {
            throw new RegraDeNegocioException("Este equipamento não está disponível no momento.", "ERR_EQUIP_001");
        }

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário autenticado não encontrado."));

        equipment.setStatus(EquipmentStatus.EM_USO);
        equipmentRepository.save(equipment);

        Loan loan = new Loan();
        loan.setEquipment(equipment);
        loan.setUser(user);
        loan.setDataRetirada(LocalDateTime.now());
        loan.setDataDevolucaoPrevista(dto.dataDevolucaoPrevista());
        loan.setStatus(LoanStatus.ATIVO);

        Loan saved = loanRepository.save(loan);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<LoanResponseDTO> findAll() {
        return loanRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional
    public LoanResponseDTO returnEquipment(UUID loanId) {
        Loan loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Empréstimo não encontrado."));

        if (loan.getStatus() == LoanStatus.DEVOLVIDO) {
            throw new RegraDeNegocioException("Este empréstimo já foi encerrado com devolução.");
        }

        Equipment equipment = loan.getEquipment();
        equipment.setStatus(EquipmentStatus.DISPONIVEL);
        equipmentRepository.save(equipment);

        loan.setDataDevolucaoReal(LocalDateTime.now());
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
                loan.getDataRetirada().toLocalDate(),
                loan.getDataDevolucaoPrevista(),
                loan.getDataDevolucaoReal() != null ? loan.getDataDevolucaoReal().toLocalDate() : null,
                loan.getStatus()
        );
    }
}