package com.example.lab_manager.service;

import com.example.lab_manager.dto.response.CheckInResponseDTO;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.exceptions.ReservaNaoConfirmadaException;
import com.example.lab_manager.model.CheckIn;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.repository.CheckInRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final ReserveService reserveService;

    private static final long TOLERANCIA_MINUTOS_ANTES = 15;

    @Transactional
    public CheckInResponseDTO realizarCheckIn(String codigoQr) {
        Reserve reserve = reserveService.buscarEntidadePorCodigoQr(codigoQr);

        if (reserve.getStatus() != ReserveStatus.CONFIRMADA) {
            throw new ReservaNaoConfirmadaException(
                    "Esta reserva ainda não foi confirmada por e-mail, check-in não é permitido.");
        }

        LocalDateTime agora = LocalDateTime.now();
        LocalDateTime inicioComTolerancia = reserve.getDataHoraInicio().minusMinutes(TOLERANCIA_MINUTOS_ANTES);

        if (agora.isBefore(inicioComTolerancia) || agora.isAfter(reserve.getDataHoraFim())) {
            throw new RegraDeNegocioException("Fora da janela de horário permitida para check-in.");
        }

        if (checkInRepository.findByReserveId(reserve.getId()).isPresent()) {
            throw new RegraDeNegocioException("Check-in já foi realizado para esta reserva.");
        }

        CheckIn checkIn = CheckIn.builder()
                .reserve(reserve)
                .horarioChegada(agora)
                .build();

        reserve.setStatus(ReserveStatus.EM_ANDAMENTO);

        return CheckInResponseDTO.fromEntity(checkInRepository.save(checkIn));
    }
}
