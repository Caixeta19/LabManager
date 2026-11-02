package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.ReserveRequestDTO;
import com.example.lab_manager.dto.response.ReserveResponseDTO;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.exceptions.HorarioConflitanteException;
import com.example.lab_manager.exceptions.LabManagerException;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.example.lab_manager.repository.ReserveRepository;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.lab_manager.support.TestData.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReserveServiceTest {

    @Mock ReserveRepository reserveRepository;
    @Mock LaboratoryRepository laboratoryRepository;
    @Mock CurrentUserService currentUserService;
    @Mock QrCodeService qrCodeService;

    ReserveService service;
    User aluno;
    Laboratory lab;
    LocalDateTime inicio = AGORA.plusDays(1);
    LocalDateTime fim = AGORA.plusDays(1).plusHours(2);

    @BeforeEach
    void setUp() {
        service = new ReserveService(reserveRepository, laboratoryRepository, currentUserService, qrCodeService, TestData.CLOCK);
        aluno = TestData.user(UserType.ALUNO);
        lab = TestData.lab(true);
    }

    private ReserveRequestDTO request(LocalDateTime ini, LocalDateTime f) {
        return new ReserveRequestDTO(lab.getId(), ini, f);
    }

    @Nested
    class Criacao {

        @Test
        void criaReservaPendenteComTokenEQrIndependentes() {
            when(laboratoryRepository.findByIdForUpdate(lab.getId())).thenReturn(Optional.of(lab));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(aluno);
            when(reserveRepository.existsConflictingReserve(eq(lab.getId()), eq(inicio), eq(fim), anyCollection())).thenReturn(false);
            when(reserveRepository.save(any(Reserve.class))).thenAnswer(inv -> inv.getArgument(0));

            ReserveResponseDTO dto = service.create(request(inicio, fim));

            ArgumentCaptor<Reserve> captor = ArgumentCaptor.forClass(Reserve.class);
            verify(reserveRepository).save(captor.capture());
            Reserve salva = captor.getValue();
            assertThat(salva.getStatus()).isEqualTo(ReserveStatus.PENDENTE);
            assertThat(salva.getTokenConfirmacao()).isNotBlank();
            assertThat(salva.getCodigoQr()).isNotBlank().isNotEqualTo(salva.getTokenConfirmacao()); // RN12
            assertThat(salva.isEmailConfirmacaoEnviado()).isFalse();
            assertThat(dto.status()).isEqualTo(ReserveStatus.PENDENTE);
            assertThat(dto.userId()).isEqualTo(aluno.getId());
        }

        @Test
        void conflitoDeHorarioLancaHorarioConflitante() {
            when(laboratoryRepository.findByIdForUpdate(lab.getId())).thenReturn(Optional.of(lab));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(aluno);
            when(reserveRepository.existsConflictingReserve(eq(lab.getId()), eq(inicio), eq(fim),
                    eq(ReserveRepository.STATUS_IGNORADOS_NO_CONFLITO))).thenReturn(true);

            assertThatThrownBy(() -> service.create(request(inicio, fim)))
                    .isInstanceOf(HorarioConflitanteException.class)
                    .hasMessage("Já existe uma reserva para este laboratório no horário solicitado.")
                    .extracting("codigoInterno", "httpStatus")
                    .containsExactly("ERR_RESERVA_001", HttpStatus.CONFLICT);
            verify(reserveRepository, never()).save(any());
        }

        @Test
        void conflitoIgnoraCanceladaEExpirada() {
            assertThat(ReserveRepository.STATUS_IGNORADOS_NO_CONFLITO)
                    .containsExactlyInAnyOrder(ReserveStatus.CANCELADA, ReserveStatus.EXPIRADA);
        }

        @Test
        void fimIgualAoInicioLancaRegraDeNegocio400() {
            assertThatThrownBy(() -> service.create(request(inicio, inicio)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("O horário de fim deve ser após o horário de início.")
                    .extracting("httpStatus").isEqualTo(HttpStatus.BAD_REQUEST);
            verifyNoInteractions(laboratoryRepository, reserveRepository);
        }

        @Test
        void fimAntesDoInicioLancaRegraDeNegocio() {
            assertThatThrownBy(() -> service.create(request(fim, inicio)))
                    .isInstanceOf(RegraDeNegocioException.class);
        }

        @Test
        void laboratorioInativoLancaErrLab001() {
            Laboratory inativo = TestData.lab(false);
            when(laboratoryRepository.findByIdForUpdate(inativo.getId())).thenReturn(Optional.of(inativo));

            assertThatThrownBy(() -> service.create(new ReserveRequestDTO(inativo.getId(), inicio, fim)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .extracting("codigoInterno", "httpStatus")
                    .containsExactly("ERR_LAB_001", HttpStatus.BAD_REQUEST);
        }

        @Test
        void usaLockPessimistaNoLaboratorio() {
            when(laboratoryRepository.findByIdForUpdate(lab.getId())).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.create(request(inicio, fim)))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            verify(laboratoryRepository).findByIdForUpdate(lab.getId());
            verify(laboratoryRepository, never()).findById(any());
        }
    }

    @Nested
    class Confirmacao {

        @Test
        void tokenValidoConfirmaEGravaData() {
            Reserve r = TestData.reserve(aluno, lab, ReserveStatus.PENDENTE, inicio, fim);
            when(reserveRepository.findByTokenConfirmacao("tok")).thenReturn(Optional.of(r));

            service.confirmByToken("tok");

            assertThat(r.getStatus()).isEqualTo(ReserveStatus.CONFIRMADA);
            assertThat(r.getDataConfirmacao()).isEqualTo(AGORA);
            verify(reserveRepository).save(r);
        }

        @Test
        void tokenInvalidoLancaRecursoNaoEncontrado() {
            when(reserveRepository.findByTokenConfirmacao("x")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.confirmByToken("x"))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @ParameterizedTest
        @EnumSource(value = ReserveStatus.class, names = {"CONFIRMADA", "CANCELADA", "EXPIRADA"})
        void reservaForaDePendenteLancaErrReserva002Com409(ReserveStatus status) {
            Reserve r = TestData.reserve(aluno, lab, status, inicio, fim);
            when(reserveRepository.findByTokenConfirmacao("tok")).thenReturn(Optional.of(r));

            assertThatThrownBy(() -> service.confirmByToken("tok"))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Esta reserva não pode mais ser confirmada.")
                    .extracting("codigoInterno", "httpStatus")
                    .containsExactly("ERR_RESERVA_002", HttpStatus.CONFLICT);
            assertThat(r.getStatus()).isEqualTo(status);
            verify(reserveRepository, never()).save(any());
        }
    }

    @Nested
    class Cancelamento {

        @ParameterizedTest
        @EnumSource(value = ReserveStatus.class, names = {"PENDENTE", "CONFIRMADA"})
        void donoCancelaPendenteOuConfirmada(ReserveStatus status) {
            Reserve r = TestData.reserve(aluno, lab, status, inicio, fim);
            when(reserveRepository.findById(r.getId())).thenReturn(Optional.of(r));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(aluno);

            service.cancel(r.getId());

            assertThat(r.getStatus()).isEqualTo(ReserveStatus.CANCELADA);
            verify(reserveRepository).save(r);
        }

        @Test
        void adminCancelaReservaDeOutro() {
            Reserve r = TestData.reserve(aluno, lab, ReserveStatus.PENDENTE, inicio, fim);
            when(reserveRepository.findById(r.getId())).thenReturn(Optional.of(r));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(UserType.ADMIN));

            service.cancel(r.getId());

            assertThat(r.getStatus()).isEqualTo(ReserveStatus.CANCELADA);
        }

        @ParameterizedTest
        @EnumSource(value = UserType.class, names = {"ALUNO", "PROFESSOR"})
        void quemNaoEDonoNemAdminRecebe404(UserType tipo) {
            Reserve r = TestData.reserve(aluno, lab, ReserveStatus.PENDENTE, inicio, fim);
            when(reserveRepository.findById(r.getId())).thenReturn(Optional.of(r));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(tipo));

            assertThatThrownBy(() -> service.cancel(r.getId()))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
            assertThat(r.getStatus()).isEqualTo(ReserveStatus.PENDENTE);
        }

        @ParameterizedTest
        @EnumSource(value = ReserveStatus.class, names = {"CANCELADA", "EXPIRADA"})
        void estadoFinalNaoPodeSerCancelado(ReserveStatus status) {
            Reserve r = TestData.reserve(aluno, lab, status, inicio, fim);
            when(reserveRepository.findById(r.getId())).thenReturn(Optional.of(r));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(aluno);

            assertThatThrownBy(() -> service.cancel(r.getId()))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .extracting("codigoInterno").isEqualTo("ERR_RESERVA_003");
        }
    }

    @Nested
    class Acesso {

        @ParameterizedTest
        @EnumSource(value = UserType.class, names = {"ADMIN", "PROFESSOR"})
        void adminEProfessorVeemReservaDeOutro(UserType tipo) {
            Reserve r = TestData.reserve(aluno, lab, ReserveStatus.PENDENTE, inicio, fim);
            when(reserveRepository.findById(r.getId())).thenReturn(Optional.of(r));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(tipo));

            assertThat(service.findById(r.getId()).id()).isEqualTo(r.getId());
        }

        @Test
        void outroAlunoRecebe404AoBuscarReserva() {
            Reserve r = TestData.reserve(aluno, lab, ReserveStatus.PENDENTE, inicio, fim);
            when(reserveRepository.findById(r.getId())).thenReturn(Optional.of(r));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(UserType.ALUNO));

            assertThatThrownBy(() -> service.findById(r.getId()))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }

        @Test
        void soODonoObtemOQrCode() {
            Reserve r = TestData.reserve(aluno, lab, ReserveStatus.CONFIRMADA, inicio, fim);
            when(reserveRepository.findById(r.getId())).thenReturn(Optional.of(r));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(aluno);
            when(qrCodeService.gerarImagemPng(r.getCodigoQr())).thenReturn(new byte[]{1, 2});

            assertThat(service.generateQrCodeImage(r.getId())).containsExactly(1, 2);
        }

        @ParameterizedTest
        @EnumSource(UserType.class)
        void quemNaoEDonoRecebe404NoQrCode(UserType tipo) {
            Reserve r = TestData.reserve(aluno, lab, ReserveStatus.CONFIRMADA, inicio, fim);
            when(reserveRepository.findById(r.getId())).thenReturn(Optional.of(r));
            when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(tipo));

            assertThatThrownBy(() -> service.generateQrCodeImage(r.getId()))
                    .isInstanceOf(LabManagerException.class)
                    .extracting("httpStatus").isEqualTo(HttpStatus.NOT_FOUND);
            verifyNoInteractions(qrCodeService);
        }
    }
}
