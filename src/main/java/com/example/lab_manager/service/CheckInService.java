package com.example.lab_manager.service;

import com.example.lab_manager.dto.response.CheckInResponseDTO;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.exceptions.ReservaNaoConfirmadaException;
import com.example.lab_manager.model.CheckIn;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.CheckInRepository;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.example.lab_manager.repository.ReserveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CheckInService {

    static final String MSG_QR_INVALIDO = "QR Code inválido ou fora do horário permitido.";
    static final String MSG_NAO_CONFIRMADA = "Sua reserva ainda não foi confirmada. Verifique seu e-mail.";
    static final String MSG_DUPLICADO = "Check-in já realizado para esta reserva.";

    /** QR do laboratório: o check-in abre 15 minutos antes do início da reserva. */
    static final Duration TOLERANCIA_ANTES_DO_INICIO = Duration.ofMinutes(15);
    private static final Set<ReserveStatus> STATUS_CHECKIN_LAB = EnumSet.of(ReserveStatus.PENDENTE, ReserveStatus.CONFIRMADA);

    private final CheckInRepository checkInRepository;
    private final ReserveRepository reserveRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    /**
     * Fluxo antigo: QR da reserva. Ordem das verificações (decisão 9):
     * <ol>
     *   <li>QR inexistente, reserva de outro usuário (exceto para ADMIN) ou fora de [início, fim] → 404 ERR_CHECKIN_002 (mensagem genérica, RN19)</li>
     *   <li>status diferente de CONFIRMADA → 422 ERR_CHECKIN_001 (RN18)</li>
     *   <li>check-in já existente → 409 ERR_CHECKIN_003 (decisão 8 / RN20)</li>
     * </ol>
     */
    @Transactional
    public CheckInResponseDTO performCheckIn(String codigoQr) {
        LocalDateTime agora = LocalDateTime.now(clock);
        User usuario = currentUserService.getUsuarioAutenticado();

        Reserve reserve = reserveRepository.findByCodigoQr(codigoQr)
                .filter(r -> podeFazerCheckIn(r, usuario))                    // decisão 5: dono ou ADMIN
                .filter(r -> dentroDaJanela(r, agora))
                .orElseThrow(CheckInService::qrInvalido);

        if (reserve.getStatus() != ReserveStatus.CONFIRMADA) {
            throw new ReservaNaoConfirmadaException(MSG_NAO_CONFIRMADA);
        }

        if (checkInRepository.existsByReserveId(reserve.getId())) {
            throw checkInDuplicado();
        }

        return registrar(reserve, agora);
    }

    /**
     * QR fixo do laboratório: o usuário logado lê o QR da porta e o sistema acha a reserva dele.
     * <ol>
     *   <li>token inexistente ou nenhuma reserva PENDENTE/CONFIRMADA do usuário nesse laboratório
     *       com "agora" em [início - 15 min, fim] → 404 ERR_CHECKIN_002 (mensagem genérica)</li>
     *   <li>só reservas PENDENTE na janela → 422 ERR_CHECKIN_001</li>
     *   <li>todas as CONFIRMADA da janela já têm check-in → 409 ERR_CHECKIN_003</li>
     * </ol>
     * Vale para qualquer papel (inclusive ADMIN), sempre sobre as reservas do próprio usuário.
     */
    @Transactional
    public CheckInResponseDTO performCheckInNoLaboratorio(String checkinToken) {
        LocalDateTime agora = LocalDateTime.now(clock);
        User usuario = currentUserService.getUsuarioAutenticado();

        Laboratory lab = laboratoryRepository.findByCheckinToken(checkinToken)
                .orElseThrow(CheckInService::qrInvalido);

        List<Reserve> naJanela = reserveRepository
                .findByUserIdAndLaboratoryIdAndStatusInAndDataHoraFimGreaterThanEqualOrderByDataHoraInicio(
                        usuario.getId(), lab.getId(), STATUS_CHECKIN_LAB, agora)
                .stream()
                .filter(r -> dentroDaJanelaComTolerancia(r, agora))
                .toList();

        if (naJanela.isEmpty()) {
            throw qrInvalido();
        }

        List<Reserve> confirmadas = naJanela.stream()
                .filter(r -> r.getStatus() == ReserveStatus.CONFIRMADA)
                .toList();
        if (confirmadas.isEmpty()) {
            throw new ReservaNaoConfirmadaException(MSG_NAO_CONFIRMADA);
        }

        Reserve reserve = confirmadas.stream()
                .filter(r -> !checkInRepository.existsByReserveId(r.getId()))
                .findFirst()
                .orElseThrow(CheckInService::checkInDuplicado);

        return registrar(reserve, agora);
    }

    private CheckInResponseDTO registrar(Reserve reserve, LocalDateTime agora) {
        CheckIn checkIn = new CheckIn();
        checkIn.setReserve(reserve);
        checkIn.setHorarioChegada(agora);

        CheckIn saved;
        try {
            // flush imediato: a constraint UNIQUE(reserve_id) pega check-ins simultâneos
            saved = checkInRepository.saveAndFlush(checkIn);
        } catch (DataIntegrityViolationException e) {
            throw checkInDuplicado();
        }

        return new CheckInResponseDTO(
                saved.getId(),
                reserve.getId(),
                saved.getHorarioChegada(),
                "Check-in realizado com sucesso!"
        );
    }

    private static boolean podeFazerCheckIn(Reserve r, User usuario) {
        return usuario.getType() == UserType.ADMIN || r.getUser().getId().equals(usuario.getId());
    }

    private static boolean dentroDaJanela(Reserve r, LocalDateTime agora) {
        return !agora.isBefore(r.getDataHoraInicio()) && !agora.isAfter(r.getDataHoraFim());
    }

    private static boolean dentroDaJanelaComTolerancia(Reserve r, LocalDateTime agora) {
        return !agora.isBefore(r.getDataHoraInicio().minus(TOLERANCIA_ANTES_DO_INICIO))
                && !agora.isAfter(r.getDataHoraFim());
    }

    private static RecursoNaoEncontradoException qrInvalido() {
        return new RecursoNaoEncontradoException(MSG_QR_INVALIDO, "ERR_CHECKIN_002");
    }

    private static RegraDeNegocioException checkInDuplicado() {
        return new RegraDeNegocioException(MSG_DUPLICADO, "ERR_CHECKIN_003", HttpStatus.CONFLICT);
    }
}
