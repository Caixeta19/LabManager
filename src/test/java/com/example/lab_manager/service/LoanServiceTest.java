package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.LoanRequestDTO;
import com.example.lab_manager.dto.response.LoanResponseDTO;
import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.enums.LoanStatus;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.Equipment;
import com.example.lab_manager.model.Loan;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.EquipmentRepository;
import com.example.lab_manager.repository.LoanRepository;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static com.example.lab_manager.support.TestData.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanServiceTest {

    @Mock LoanRepository loanRepository;
    @Mock EquipmentRepository equipmentRepository;
    @Mock CurrentUserService currentUserService;

    LoanService service;
    User aluno;
    LocalDate amanha = LocalDate.from(AGORA).plusDays(1);

    @BeforeEach
    void setUp() {
        service = new LoanService(loanRepository, equipmentRepository, currentUserService, TestData.CLOCK);
        aluno = TestData.user(UserType.ALUNO);
    }

    @Test
    void emprestimoDeEquipamentoDisponivelViraEmUso() {
        Equipment eq = TestData.equipment(EquipmentStatus.DISPONIVEL);
        when(equipmentRepository.findByIdForUpdate(eq.getId())).thenReturn(Optional.of(eq));
        when(currentUserService.getUsuarioAutenticado()).thenReturn(aluno);
        when(loanRepository.save(any(Loan.class))).thenAnswer(inv -> inv.getArgument(0));

        LoanResponseDTO dto = service.createLoan(new LoanRequestDTO(eq.getId(), amanha));

        assertThat(eq.getStatus()).isEqualTo(EquipmentStatus.EM_USO); // RN08
        assertThat(dto.status()).isEqualTo(LoanStatus.ATIVO);
        assertThat(dto.dataDevolucaoPrevista()).isEqualTo(amanha);
        assertThat(dto.dataRetirada()).isEqualTo(AGORA);
        verify(equipmentRepository).save(eq);
    }

    @ParameterizedTest
    @EnumSource(value = EquipmentStatus.class, names = {"EM_USO", "MANUTENCAO"})
    void equipamentoIndisponivelLancaErrEquip001Com409(EquipmentStatus status) {
        Equipment eq = TestData.equipment(status);
        when(equipmentRepository.findByIdForUpdate(eq.getId())).thenReturn(Optional.of(eq));

        assertThatThrownBy(() -> service.createLoan(new LoanRequestDTO(eq.getId(), amanha)))
                .isInstanceOf(RegraDeNegocioException.class) // RN07
                .hasMessage("Este equipamento não está disponível no momento.")
                .extracting("codigoInterno", "httpStatus")
                .containsExactly("ERR_EQUIP_001", HttpStatus.CONFLICT);
        assertThat(eq.getStatus()).isEqualTo(status);
        verify(loanRepository, never()).save(any());
    }

    @Test
    void usaLockPessimistaNoEquipamento() {
        UUID id = UUID.randomUUID();
        when(equipmentRepository.findByIdForUpdate(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createLoan(new LoanRequestDTO(id, amanha)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(equipmentRepository, never()).findById(any());
    }

    @ParameterizedTest
    @EnumSource(value = LoanStatus.class, names = {"ATIVO", "ATRASADO"})
    void devolucaoVoltaEquipamentoParaDisponivel(LoanStatus status) {
        Equipment eq = TestData.equipment(EquipmentStatus.EM_USO);
        Loan loan = TestData.loan(aluno, eq, status);
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));
        when(loanRepository.save(loan)).thenReturn(loan);

        LoanResponseDTO dto = service.returnEquipment(loan.getId());

        assertThat(eq.getStatus()).isEqualTo(EquipmentStatus.DISPONIVEL); // RN09
        assertThat(loan.getDataDevolucaoReal()).isEqualTo(AGORA);
        assertThat(dto.status()).isEqualTo(LoanStatus.DEVOLVIDO);
        verify(equipmentRepository).save(eq);
    }

    @Test
    void devolverEmprestimoJaDevolvidoLancaErrLoan001() {
        Equipment eq = TestData.equipment(EquipmentStatus.DISPONIVEL);
        Loan loan = TestData.loan(aluno, eq, LoanStatus.DEVOLVIDO);
        when(loanRepository.findById(loan.getId())).thenReturn(Optional.of(loan));

        assertThatThrownBy(() -> service.returnEquipment(loan.getId()))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting("codigoInterno").isEqualTo("ERR_LOAN_001");
        verify(equipmentRepository, never()).save(any());
    }
}
