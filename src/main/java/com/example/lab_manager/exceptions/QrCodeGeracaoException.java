package com.example.lab_manager.exceptions;

import org.springframework.http.HttpStatus;

/** Falha do ZXing ao gerar a imagem do QR Code → 500 com mensagem genérica. */
public class QrCodeGeracaoException extends LabManagerException {
    public QrCodeGeracaoException(Throwable cause) {
        super("Não foi possível gerar o QR Code da reserva.", "ERR_QRCODE_001",
                HttpStatus.INTERNAL_SERVER_ERROR, cause);
    }
}
