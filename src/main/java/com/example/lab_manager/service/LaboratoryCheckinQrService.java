package com.example.lab_manager.service;

import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.model.CheckinTokens;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.repository.LaboratoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * QR fixo de check-in do laboratório (só ADMIN). O QR leva a URL do front
 * {@code APP_FRONT_URL/check-in?lab={token}}: lido pela tela de check-in ou pela câmera do celular.
 */
@Service
public class LaboratoryCheckinQrService {

    private final LaboratoryRepository laboratoryRepository;
    private final QrCodeService qrCodeService;
    private final String frontUrl;

    public LaboratoryCheckinQrService(LaboratoryRepository laboratoryRepository,
                                      QrCodeService qrCodeService,
                                      @Value("${app.front-url}") String frontUrl) {
        this.laboratoryRepository = laboratoryRepository;
        this.qrCodeService = qrCodeService;
        this.frontUrl = frontUrl.endsWith("/") ? frontUrl.substring(0, frontUrl.length() - 1) : frontUrl;
    }

    @Transactional(readOnly = true)
    public byte[] gerarImagemPng(UUID laboratoryId) {
        return qrCodeService.gerarImagemPng(conteudoDoQr(buscar(laboratoryId).getCheckinToken()));
    }

    /** Troca o token: o QR impresso antes deixa de funcionar. */
    @Transactional
    public void regenerarToken(UUID laboratoryId) {
        Laboratory lab = buscar(laboratoryId);
        lab.setCheckinToken(CheckinTokens.novo());
        laboratoryRepository.save(lab);
    }

    String conteudoDoQr(String token) {
        return frontUrl + "/check-in?lab=" + token;
    }

    private Laboratory buscar(UUID id) {
        return laboratoryRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Laboratório não encontrado."));
    }
}
