package com.example.lab_manager.dto.error;

import org.springframework.http.HttpStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Corpo padrão de erro da API (decisão 13):
 * {@code timestamp, status, codigo, mensagem, path, erros[] {campo, mensagem}}.
 */
public record ErrorResponseDTO(
        OffsetDateTime timestamp,
        int status,
        String codigo,
        String mensagem,
        String path,
        List<CampoErroDTO> erros
) {
    public record CampoErroDTO(String campo, String mensagem) {}

    public static ErrorResponseDTO of(HttpStatus status, String codigo, String mensagem, String path) {
        return of(status, codigo, mensagem, path, List.of());
    }

    public static ErrorResponseDTO of(HttpStatus status, String codigo, String mensagem, String path,
                                      List<CampoErroDTO> erros) {
        return new ErrorResponseDTO(OffsetDateTime.now(), status.value(), codigo, mensagem, path, erros);
    }
}
