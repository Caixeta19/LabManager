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
import com.example.lab_manager.repository.ReserveRepository;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static com.example.lab_manager.support.TestData.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/** Ordem das verificações do check-in (decisão 9): 404 → 422 → 409. */
@ExtendWith(MockitoExtension.class)
class CheckInServiceTest {

    private static final String QR = "codigo-qr";

    @Mock CheckInRepository checkInRepository;
    @Mock ReserveRepository reserveRepository;
    @Mock CurrentUserService currentUserService;

    CheckInService service;
    User dono;
    Laboratory lab;

    @BeforeEach
    void setUp() {
        service = new CheckInService(checkInRepository, reserveRepository, currentUserService, TestData.CLOCK);
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
    void adminNaoFazCheckInDeOutro() {
        when(currentUserService.getUsuarioAutenticado()).thenReturn(TestData.user(UserType.ADMIN));
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
}
