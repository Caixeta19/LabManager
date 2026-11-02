package com.example.lab_manager.service;

import com.example.lab_manager.dto.response.CheckInResponseDTO;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.exceptions.ReservaNaoConfirmadaException;
import com.example.lab_manager.model.CheckIn;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.CheckInRepository;
import com.example.lab_manager.repository.ReserveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CheckInService {

    static final String MSG_QR_INVALIDO = "QR Code inválido ou fora do horário permitido.";
    static final String MSG_NAO_CONFIRMADA = "Sua reserva ainda não foi confirmada. Verifique seu e-mail.";
    static final String MSG_DUPLICADO = "Check-in já realizado para esta reserva.";

    private final CheckInRepository checkInRepository;
    private final ReserveRepository reserveRepository;
    private final CurrentUserService currentUserService;
    private final Clock clock;

    /**
     * Ordem das verificações (decisão 9):
     * <ol>
     *   <li>QR inexistente, reserva de outro usuário ou fora de [início, fim] → 404 ERR_CHECKIN_002 (mensagem genérica, RN19)</li>
     *   <li>status diferente de CONFIRMADA → 422 ERR_CHECKIN_001 (RN18)</li>
     *   <li>check-in já existente → 409 ERR_CHECKIN_003 (decisão 8 / RN20)</li>
     * </ol>
     */
    @Transactional
    public CheckInResponseDTO performCheckIn(String codigoQr) {
        LocalDateTime agora = LocalDateTime.now(clock);
        User usuario = currentUserService.getUsuarioAutenticado();

        Reserve reserve = reserveRepository.findByCodigoQr(codigoQr)
                .filter(r -> r.getUser().getId().equals(usuario.getId()))      // decisão 5: só o dono
                .filter(r -> dentroDaJanela(r, agora))
                .orElseThrow(() -> new RecursoNaoEncontradoException(MSG_QR_INVALIDO, "ERR_CHECKIN_002"));

        if (reserve.getStatus() != ReserveStatus.CONFIRMADA) {
            throw new ReservaNaoConfirmadaException(MSG_NAO_CONFIRMADA);
        }

        if (checkInRepository.existsByReserveId(reserve.getId())) {
            throw checkInDuplicado();
        }

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

    private static boolean dentroDaJanela(Reserve r, LocalDateTime agora) {
        return !agora.isBefore(r.getDataHoraInicio()) && !agora.isAfter(r.getDataHoraFim());
    }

    private static RegraDeNegocioException checkInDuplicado() {
        return new RegraDeNegocioException(MSG_DUPLICADO, "ERR_CHECKIN_003", HttpStatus.CONFLICT);
    }
}
