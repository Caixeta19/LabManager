package com.example.lab_manager.service;
import com.example.lab_manager.dto.request.ReserveRequestDTO;
import com.example.lab_manager.dto.response.ReserveResponseDTO;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.exceptions.HorarioConflitanteException;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.ReserveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ReserveService {

    private final ReserveRepository reserveRepository;
    private final UserService userService;
    private final LaboratoryService laboratoryService;

    @Transactional
    public ReserveResponseDTO criar(ReserveRequestDTO dto) {
        if (!dto.dataHoraFim().isAfter(dto.dataHoraInicio())) {
            throw new RegraDeNegocioException("O horário de fim deve ser após o horário de início.");
        }

        User user = userService.buscarEntidadePorId(dto.userId());
        Laboratory laboratory = laboratoryService.buscarEntidadePorId(dto.laboratoryId());

        validarConflitoDeHorario(dto.laboratoryId(), dto.dataHoraInicio(), dto.dataHoraFim());

        Reserve reserve = Reserve.builder()
                .user(user)
                .laboratory(laboratory)
                .dataHoraInicio(dto.dataHoraInicio())
                .dataHoraFim(dto.dataHoraFim())
                .build();

        return ReserveResponseDTO.fromEntity(reserveRepository.save(reserve));
    }

    private void validarConflitoDeHorario(UUID laboratoryId, LocalDateTime inicio, LocalDateTime fim) {
        List<Reserve> conflitantes = reserveRepository
                .findByLaboratoryIdAndDataHoraInicioLessThanAndDataHoraFimGreaterThan(
                        laboratoryId, fim, inicio);

        boolean existeConflito = conflitantes.stream()
                .anyMatch(r -> r.getStatus() != ReserveStatus.CANCELADA
                        && r.getStatus() != ReserveStatus.EXPIRADA);

        if (existeConflito) {
            throw new HorarioConflitanteException(
                    "Já existe uma reserva para este laboratório no horário solicitado.");
        }
    }

    @Transactional
    public ReserveResponseDTO confirmar(String token) {
        Reserve reserve = reserveRepository.findByConfirmationToken(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Token de confirmação inválido."));

        if (reserve.getStatus() != ReserveStatus.PENDENTE) {
            throw new RegraDeNegocioException("Esta reserva não pode mais ser confirmada (status atual: "
                    + reserve.getStatus() + ").");
        }

        reserve.setStatus(ReserveStatus.CONFIRMADA);
        reserve.setDataConfirmacao(LocalDateTime.now());

        return ReserveResponseDTO.fromEntity(reserveRepository.save(reserve));
    }

    @Transactional
    public void cancelar(UUID id) {
        Reserve reserve = buscarEntidadePorId(id);
        reserve.setStatus(ReserveStatus.CANCELADA);
        reserveRepository.save(reserve);
    }

    @Transactional(readOnly = true)
    public ReserveResponseDTO buscarPorId(UUID id) {
        return ReserveResponseDTO.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public List<ReserveResponseDTO> listarPorUsuario(UUID userId) {
        return reserveRepository.findByUserId(userId).stream()
                .map(ReserveResponseDTO::fromEntity)
                .toList();
    }

    Reserve buscarEntidadePorId(UUID id) {
        return reserveRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada: " + id));
    }

    Reserve buscarEntidadePorCodigoQr(String codigoQr) {
        return reserveRepository.findByQRCode(codigoQr)
                .orElseThrow(() -> new RecursoNaoEncontradoException("QR Code inválido."));
    }
}