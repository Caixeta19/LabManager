package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.ReserveRequestDTO;
import com.example.lab_manager.dto.response.ReserveResponseDTO;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.exceptions.HorarioConflitanteException;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.*;
import com.example.lab_manager.repository.LaboratoryRepository;
import com.example.lab_manager.repository.ReserveRepository;
import com.example.lab_manager.repository.UserRepository;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReserveService {

    private final ReserveRepository reserveRepository;
    private final LaboratoryRepository laboratoryRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReserveResponseDTO create(ReserveRequestDTO dto) {
        if (!dto.dataHoraFim().isAfter(dto.dataHoraInicio())) {
            throw new RegraDeNegocioException("O horário de fim deve ser após o horário de início.");
        }

        Laboratory lab = laboratoryRepository.findById(dto.laboratoryId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório não encontrado."));

        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário autenticado não encontrado."));

        boolean hasConflict = reserveRepository.existsConflictingReserve(
                lab.getId(), dto.dataHoraInicio(), dto.dataHoraFim()
        );
        if (hasConflict) {
            throw new HorarioConflitanteException("Já existe uma reserva para este laboratório no horário solicitado.");
        }

        Reserve reserve = new Reserve();
        reserve.setLaboratory(lab);
        reserve.setUser(user);
        reserve.setDataHoraInicio(dto.dataHoraInicio());
        reserve.setDataHoraFim(dto.dataHoraFim());
        reserve.setStatus(ReserveStatus.PENDENTE);
        reserve.setEmailConfirmacaoEnviado(false);

        Reserve saved = reserveRepository.save(reserve);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ReserveResponseDTO> findAll() {
        return reserveRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReserveResponseDTO findById(UUID id) {
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));
        return toDTO(reserve);
    }

    @Transactional
    public void cancel(UUID id) {
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));

        if (reserve.getStatus() == ReserveStatus.CANCELADA || reserve.getStatus() == ReserveStatus.EXPIRADA) {
            throw new RegraDeNegocioException("Esta reserva já está finalizada ou cancelada.");
        }

        reserve.setStatus(ReserveStatus.CANCELADA);
        reserveRepository.save(reserve);
    }

    @Transactional
    public void confirmByToken(String token) {
        Reserve reserve = reserveRepository.findByTokenConfirmacao(token)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Token de confirmação inválido."));

        if (reserve.getStatus() != ReserveStatus.PENDENTE) {
            throw new RegraDeNegocioException("Esta reserva não pode mais ser confirmada.", "ERR_RESERVA_002");
        }

        reserve.setStatus(ReserveStatus.CONFIRMADA);
        reserve.setDataConfirmacao(LocalDateTime.now());
        reserveRepository.save(reserve);
    }

    @Transactional(readOnly = true)
    public byte[] generateQrCodeImage(UUID id) {
        Reserve reserve = reserveRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Reserva não encontrada."));

        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            var bitMatrix = qrCodeWriter.encode(reserve.getCodigoQr(), BarcodeFormat.QR_CODE, 250, 250);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new RuntimeException("Erro ao gerar imagem do QR Code.", e);
        }
    }

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
                r.getDataConfirmacao()
        );
    }
}