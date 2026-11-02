package com.example.lab_manager.service;

import com.example.lab_manager.exceptions.QrCodeGeracaoException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/** Gera a imagem PNG do QR Code sob demanda, sem armazenar nada (ADR-06). */
@Service
public class QrCodeService {

    private static final int TAMANHO_PX = 250;

    public byte[] gerarImagemPng(String conteudo) {
        try {
            BitMatrix bitMatrix = new QRCodeWriter().encode(conteudo, BarcodeFormat.QR_CODE, TAMANHO_PX, TAMANHO_PX);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return outputStream.toByteArray();
        } catch (WriterException | IOException e) {
            throw new QrCodeGeracaoException(e);
        }
    }
}
