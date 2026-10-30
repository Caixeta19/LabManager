package com.example.lab_manager.service;

import com.example.lab_manager.dto.response.CheckInResponseDTO;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.ReservaNaoConfirmadaException;
import com.example.lab_manager.model.CheckIn;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.repository.CheckInRepository;
import com.example.lab_manager.repository.ReserveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class CheckInService {

    private final CheckInRepository checkInRepository;
    private final ReserveRepository reserveRepository;

    @Transactional
    public CheckInResponseDTO performCheckIn(String codigoQr) {
        LocalDateTime agora = LocalDateTime.now();

        Reserve reserve = reserveRepository.findByQRCode(codigoQr)
                .orElseThrow(() -> new RecursoNaoEncontradoException("QR Code inválido ou fora do horário permitido."));

        if (reserve.getStatus() != ReserveStatus.CONFIRMADA) {
            throw new ReservaNaoConfirmadaException("Sua reserva ainda não foi confirmada. Verifique seu e-mail.");
        }

        if (agora.isBefore(reserve.getDataHorainicio()) || agora.isAfter(reserve.getDataHoraFim())) {
            throw new RecursoNaoEncontradoException("QR Code inválido ou fora do horário permitido.");
        }

        if (checkInRepository.existsByReserveId(reserve.getId())) {
            throw new ReservaNaoConfirmadaException("O check-in para esta reserva já foi realizado.");
        }

        CheckIn checkIn = new CheckIn();
        checkIn.setReserve(reserve);
        checkIn.setHorarioChegada(agora);

        CheckIn saved = checkInRepository.save(checkIn);

        return new CheckInResponseDTO(
                saved.getId(),
                reserve.getId(),
                saved.getHorarioChegada(),
                "Check-in realizado com sucesso!"
        );
    }
}