package com.example.lab_manager.service;

import com.example.lab_manager.dto.response.CheckInResponseDTO;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.exceptions.LabManagerException;
import com.example.lab_manager.exceptions.ReservaNaoConfirmadaException;
import com.example.lab_manager.model.CheckIn;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.CheckInRepository;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.example.lab_manager.repository.ReserveRepository;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.example.lab_manager.support.TestData.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/** Ordem das verificações do check-in (decisão 9): 404 → 422 → 409. */
@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    private static final String QR = "codigo-qr";

    @Mock CheckInRepository checkInRepository;
    @Mock ReserveRepository reserveRepository;
    @Mock CurrentUserService currentUserService;
    @Mock LaboratoryRepository laboratoryRepository;

    CheckInService service;
    User dono;
    Laboratory lab;

    @BeforeEach
    void setUp() {
        service = new CheckInService(checkInRepository, reserveRepository, laboratoryRepository, currentUserService, TestData.CLOCK);
        dono = TestData.user(UserType.ALUNO);
        lab = TestData.lab(true);
    }

    private Reserve reserva(ReserveStatus status, LocalDateTime inicio, LocalDateTime fim) {
        Reserve r = TestData.reserve(dono, lab, status, inicio, fim);
        when(reserveRepository.findByCodigoQr(QR)).thenReturn(Optional.of(r));
        return r;
    }

    private Reserve reservaNaJanela(ReserveStatus status) {
        return reserva(status, AGORA.minusMinutes(10), AGORA.plusMinutes(50));
    }

    private void assertErro(HttpStatus status, String codigo) {
        assertThatThrownBy(() -> service.performCheckIn(QR))
                .isInstanceOf(LabManagerException.class)
                .extracting("httpStatus", "codigoInterno")
                .containsExactly(status, codigo);
    }

    @Test
    void sucessoDoDonoComReservaConfirmadaNaJanela() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(dono);
        Reserve r = reservaNaJanela(ReserveStatus.CONFIRMADA);
        when(checkInRepository.existsByReserveId(r.getId())).thenReturn(false);
        when(checkInRepository.saveAndFlush(any(CheckIn.class))).thenAnswer(inv -> {
            CheckIn c = inv.getArgument(0);
            c.setId(UUID.randomUUID());
            return c;
        });

        CheckInResponseDTO dto = service.performCheckIn(QR);

        assertThat(dto.reserveId()).isEqualTo(r.getId());
        assertThat(dto.dataHoraCheckIn()).isEqualTo(AGORA);
    }

    @Test
    void limitesDaJanelaSaoInclusivos() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(dono);
        reserva(ReserveStatus.CONFIRMADA, AGORA, AGORA.plusHours(1));
        when(checkInRepository.saveAndFlush(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.performCheckIn(QR)).isNotNull();
    }

    // ---------- (a) 404 ERR_CHECKIN_002 ----------

    @Test
    void qrInexistente404() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(dono);
        when(reserveRepository.findByCodigoQr(QR)).thenReturn(Optional.empty());

        assertErro(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
    }

    @Test
    void reservaDeOutroUsuario404MesmoSePendente() {
        // dono diferente vem antes do status: não revela que a reserva existe nem o status dela
        when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(UserType.ALUNO));
        reservaNaJanela(ReserveStatus.PENDENTE);

        assertErro(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
    }

    @Test
    void adminFazCheckInDeReservaDeOutro() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(UserType.ADMIN));
        Reserve r = reservaNaJanela(ReserveStatus.CONFIRMADA);
        when(checkInRepository.saveAndFlush(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.performCheckIn(QR).reserveId()).isEqualTo(r.getId());
    }

    @Test
    void adminForaDaJanela404() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(UserType.ADMIN));
        reserva(ReserveStatus.CONFIRMADA, AGORA.plusMinutes(1), AGORA.plusHours(1));

        assertErro(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
    }

    @Test
    void professorNaoFazCheckInDeOutro() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(UserType.PROFESSOR));
        reservaNaJanela(ReserveStatus.CONFIRMADA);

        assertErro(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
    }

    @Test
    void antesDaJanela404MesmoSePendente() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(dono);
        reserva(ReserveStatus.PENDENTE, AGORA.plusMinutes(1), AGORA.plusHours(1));

        assertErro(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
    }

    @Test
    void depoisDaJanela404() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(dono);
        reserva(ReserveStatus.CONFIRMADA, AGORA.minusHours(2), AGORA.minusSeconds(1));

        assertErro(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
    }

    // ---------- (b) 422 ERR_CHECKIN_001 ----------

    @ParameterizedTest
    @EnumSource(value = ReserveStatus.class, names = {"PENDENTE", "CANCELADA", "EXPIRADA"})
    void naoConfirmadaNaJanela422(ReserveStatus status) {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(dono);
        reservaNaJanela(status);

        assertThatThrownBy(() -> service.performCheckIn(QR))
                .isInstanceOf(ReservaNaoConfirmadaException.class)
                .hasMessage("Sua reserva ainda não foi confirmada. Verifique seu e-mail.");
        assertErro(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_CHECKIN_001");
        verify(checkInRepository, never()).existsByReserveId(any());
    }

    // ---------- (c) 409 ERR_CHECKIN_003 ----------

    @Test
    void checkInJaExistente409() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(dono);
        Reserve r = reservaNaJanela(ReserveStatus.CONFIRMADA);
        when(checkInRepository.existsByReserveId(r.getId())).thenReturn(true);

        assertErro(HttpStatus.CONFLICT, "ERR_CHECKIN_003");
        verify(checkInRepository, never()).saveAndFlush(any());
    }

    @Test
    void corridaNaConstraintUniqueVira409() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(dono);
        Reserve r = reservaNaJanela(ReserveStatus.CONFIRMADA);
        when(checkInRepository.existsByReserveId(r.getId())).thenReturn(false);
        when(checkInRepository.saveAndFlush(any(CheckIn.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key value violates unique constraint"));

        assertErro(HttpStatus.CONFLICT, "ERR_CHECKIN_003");
    }

    // ---------- QR fixo do laboratório (POST /checkin/lab/{token}) ----------

    @Nested
    class QrDoLaboratorio {

        static final String TOKEN = "token-do-laboratorio";

        @BeforeEach
        void labDoToken() {
            lenient().when(laboratoryRepository.findByCheckinToken(TOKEN)).thenReturn(Optional.of(lab));
            lenient().when(checkInRepository.saveAndFlush(any(CheckIn.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        /** O repositório devolve estas reservas do usuário logado no laboratório do token. */
        private void reservasDoUsuario(User user, Reserve... reservas) {
            when(currentUserService.getUsuarioAutenticado()).thenReturn(user);
            when(reserveRepository.findByUserIdAndLaboratoryIdAndStatusInAndDataHoraFimGreaterThanEqualOrderByDataHoraInicio(
                    eq(user.getId()), eq(lab.getId()), anyCollection(), eq(AGORA))).thenReturn(List.of(reservas));
        }

        private Reserve reserva(ReserveStatus status, LocalDateTime inicio, LocalDateTime fim) {
            return TestData.reserve(dono, lab, status, inicio, fim);
        }

        private void assertErroLab(HttpStatus status, String codigo) {
            assertThatThrownBy(() -> service.performCheckInNoLaboratorio(TOKEN))
                    .isInstanceOf(LabManagerException.class)
                    .extracting("httpStatus", "codigoInterno")
                    .containsExactly(status, codigo);
        }

        @Test
        void sucessoDentroDaJanela() {
            Reserve r = reserva(ReserveStatus.CONFIRMADA, AGORA.minusMinutes(10), AGORA.plusMinutes(50));
            reservasDoUsuario(dono, r);

            CheckInResponseDTO dto = service.performCheckInNoLaboratorio(TOKEN);

            assertThat(dto.reserveId()).isEqualTo(r.getId());
            assertThat(dto.dataHoraCheckIn()).isEqualTo(AGORA);
        }

        @Test
        void toleranciaDe15MinutosAntesDoInicioEInclusiva() {
            Reserve r = reserva(ReserveStatus.CONFIRMADA, AGORA.plusMinutes(15), AGORA.plusHours(1));
            reservasDoUsuario(dono, r);

            assertThat(service.performCheckInNoLaboratorio(TOKEN).reserveId()).isEqualTo(r.getId());
        }

        @Test
        void maisDe15MinutosAntesDoInicio404() {
            reservasDoUsuario(dono, reserva(ReserveStatus.CONFIRMADA, AGORA.plusMinutes(16), AGORA.plusHours(1)));

            assertErroLab(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
            verify(checkInRepository, never()).saveAndFlush(any());
        }

        @Test
        void fimDaReservaEInclusivo() {
            Reserve r = reserva(ReserveStatus.CONFIRMADA, AGORA.minusHours(1), AGORA);
            reservasDoUsuario(dono, r);

            assertThat(service.performCheckInNoLaboratorio(TOKEN).reserveId()).isEqualTo(r.getId());
        }

        @Test
        void reservaEmOutroLaboratorio404() {
            // o token é do "lab"; a reserva do usuário é em outro laboratório, então a busca no "lab" vem vazia
            reservasDoUsuario(dono);

            assertErroLab(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
        }

        @Test
        void tokenInexistente404() {
            when(currentUserService.getUsuarioAutenticado()).thenReturn(dono);
            when(laboratoryRepository.findByCheckinToken("token-velho")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.performCheckInNoLaboratorio("token-velho"))
                    .isInstanceOf(LabManagerException.class)
                    .extracting("httpStatus", "codigoInterno")
                    .containsExactly(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
            verifyNoInteractions(reserveRepository);
        }

        @Test
        void reservaPendente422() {
            reservasDoUsuario(dono, reserva(ReserveStatus.PENDENTE, AGORA.minusMinutes(5), AGORA.plusHours(1)));

            assertErroLab(HttpStatus.UNPROCESSABLE_ENTITY, "ERR_CHECKIN_001");
        }

        @Test
        void pendenteForaDaJanela404NaoRevelaStatus() {
            reservasDoUsuario(dono, reserva(ReserveStatus.PENDENTE, AGORA.plusHours(3), AGORA.plusHours(4)));

            assertErroLab(HttpStatus.NOT_FOUND, "ERR_CHECKIN_002");
        }

        @Test
        void checkInDuplicado409() {
            Reserve r = reserva(ReserveStatus.CONFIRMADA, AGORA.minusMinutes(10), AGORA.plusMinutes(50));
            reservasDoUsuario(dono, r);
            when(checkInRepository.existsByReserveId(r.getId())).thenReturn(true);

            assertErroLab(HttpStatus.CONFLICT, "ERR_CHECKIN_003");
            verify(checkInRepository, never()).saveAndFlush(any());
        }

        @Test
        void comPendenteEConfirmadaNaJanelaUsaAConfirmada() {
            Reserve pendente = reserva(ReserveStatus.PENDENTE, AGORA.minusMinutes(30), AGORA.plusMinutes(5));
            Reserve confirmada = reserva(ReserveStatus.CONFIRMADA, AGORA.plusMinutes(10), AGORA.plusHours(1));
            reservasDoUsuario(dono, pendente, confirmada);

            assertThat(service.performCheckInNoLaboratorio(TOKEN).reserveId()).isEqualTo(confirmada.getId());
        }

        @Test
        void adminTambemFazCheckInDaPropriaReserva() {
            User admin = TestData.user(UserType.ADMIN);
            Reserve r = TestData.reserve(admin, lab, ReserveStatus.CONFIRMADA, AGORA.minusMinutes(1), AGORA.plusHours(1));
            reservasDoUsuario(admin, r);

            assertThat(service.performCheckInNoLaboratorio(TOKEN).reserveId()).isEqualTo(r.getId());
        }
    }
}
