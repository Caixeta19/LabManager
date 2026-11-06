package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.ReserveRequestDTO;
import com.example.lab_manager.dto.response.ReserveResponseDTO;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.exceptions.HorarioConflitanteException;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.example.lab_manager.repository.ReserveRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReserveService {

    static final String MSG_NAO_ENCONTRADA = "Reserva não encontrada.";

    private final ReserveRepository reserveRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final CurrentUserService currentUserService;
    private final QrCodeService qrCodeService;
    private final Clock clock;

    @Transactional
    public ReserveResponseDTO create(ReserveRequestDTO dto) {
        // RN10
        if (!dto.dataHoraFim().isAfter(dto.dataHoraInicio())) {
            throw new RegraDeNegocioException("O horário de fim deve ser após o horário de início.");
        }

        // Trava o laboratório: duas criações simultâneas no mesmo lab passam aqui uma de cada vez
        Laboratory lab = laboratoryRepository.findByIdForUpdate(dto.laboratoryId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório não encontrado."));

        // Decisão 11
        if (!Boolean.TRUE.equals(lab.getActive())) {
            throw new RegraDeNegocioException("Laboratório inativo.", "ERR_LAB_001");
        }

        if (lab.isMaintenance()) {
            throw new RegraDeNegocioException("Laboratório em manutenção.", "ERR_LAB_002");
        }

        User user = currentUserService.getUsuarioAutenticado();

        // RN11 / RF12
        boolean hasConflict = reserveRepository.existsConflictingReserve(
                lab.getId(), dto.dataHoraInicio(), dto.dataHoraFim(), ReserveRepository.STATUS_IGNORADOS_NO_CONFLITO);
        if (hasConflict) {
            throw new HorarioConflitanteException("Já existe uma reserva para este laboratório no horário solicitado.");
        }

        // RN12: tokenConfirmacao e codigoQr são gerados pelo @Builder.Default da entidade
        Reserve reserve = Reserve.builder()
                .laboratory(lab)
                .user(user)
                .dataHoraInicio(dto.dataHoraInicio())
                .dataHoraFim(dto.dataHoraFim())
                .reason(StringUtils.hasText(dto.reason()) ? dto.reason().trim() : null)
                .build();

        return toDTO(reserveRepository.save(reserve));
    }

    @Transactional(readOnly = true)
    public List<ReserveResponseDTO> findAll() {
        return reserveRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    /** Reservas do usuário logado, da mais recente para a mais antiga. */
    @Transactional(readOnly = true)
    public List<ReserveResponseDTO> findMine() {
        User user = currentUserService.getUsuarioAutenticado();
        return reserveRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /** Decisão 10: dono, ADMIN ou PROFESSOR. Os demais recebem 404. */
    @Transactional(readOnly = true)
    public ReserveResponseDTO findById(UUID id) {
        Reserve reserve = buscar(id);
        User user = currentUserService.getUsuarioAutenticado();
        if (!isDono(reserve, user) && user.getType() != UserType.ADMIN && user.getType() != UserType.PROFESSOR) {
            throw new RecursoNaoEncontradoException(MSG_NAO_ENCONTRADA);
        }
        return toDTO(reserve);
    }

    /** RF15 / decisão 10: dono ou ADMIN. Os demais recebem 404. */
    @Transactional
    public void cancel(UUID id) {
        Reserve reserve = buscar(id);
        User user = currentUserService.getUsuarioAutenticado();
        if (!isDono(reserve, user) && user.getType() != UserType.ADMIN) {
            throw new RecursoNaoEncontradoException(MSG_NAO_ENCONTRADA);
        }

        if (!reserve.getStatus().podeTransicionarPara(ReserveStatus.CANCELADA)) {
            throw new RegraDeNegocioException("Esta reserva já está finalizada ou cancelada.", "ERR_RESERVA_003");
        }

        reserve.setStatus(ReserveStatus.CANCELADA); // RN14: mantida no banco
        reserveRepository.save(reserve);
    }

    /** RN17: só PENDENTE vira CONFIRMADA; o link não pode ser usado duas vezes. */
    @Transactional
    public void confirmByToken(String token) {
        Reserve reserve = reserveRepository.findByTokenConfirmacao(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Token de confirmação inválido."));

        if (!reserve.getStatus().podeTransicionarPara(ReserveStatus.CONFIRMADA)) {
            throw new RegraDeNegocioException("Esta reserva não pode mais ser confirmada.",
                    "ERR_RESERVA_002", HttpStatus.CONFLICT);
        }

        reserve.setStatus(ReserveStatus.CONFIRMADA);
        reserve.setDataConfirmacao(LocalDateTime.now(clock));
        reserveRepository.save(reserve);
    }

    /** RF14 / decisão 10: só o dono obtém a imagem do QR Code. */
    @Transactional(readOnly = true)
    public byte[] generateQrCodeImage(UUID id) {
        Reserve reserve = buscar(id);
        User user = currentUserService.getUsuarioAutenticado();
        if (!isDono(reserve, user)) {
            throw new RecursoNaoEncontradoException(MSG_NAO_ENCONTRADA);
        }
        return qrCodeService.gerarImagemPng(reserve.getCodigoQr());
    }

    private Reserve buscar(UUID id) {
        return reserveRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException(MSG_NAO_ENCONTRADA));
    }

    private static boolean isDono(Reserve reserve, User user) {
        return reserve.getUser().getId().equals(user.getId());
    }

    /** RN27: o DTO nunca leva tokenConfirmacao nem codigoQr. */
    private ReserveResponseDTO toDTO(Reserve r) {
        return new ReserveResponseDTO(
                r.getId(),
                r.getLaboratory().getId(),
                r.getLaboratory().getName(),
                r.getUser().getId(),
                r.getUser().getName(),
                r.getDataHoraInicio(),
                r.getDataHoraFim(),
                r.getStatus(),
                r.getDataConfirmacao(),
                r.getUser().getType(),
                r.getReason(),
                r.getCreatedAt()
        );
    }
}
