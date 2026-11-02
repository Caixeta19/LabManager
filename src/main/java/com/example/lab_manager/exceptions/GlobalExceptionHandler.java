package com.example.lab_manager.exceptions;

import com.example.lab_manager.dto.error.ErrorResponseDTO;
import com.example.lab_manager.security.RestAccessDeniedHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;
import java.util.stream.Stream;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(LabManagerException.class)
    public ResponseEntity<ErrorResponseDTO> handleLabManagerException(LabManagerException ex, HttpServletRequest request) {
        return build(ex.getHttpStatus(), ex.getCodigoInterno(), ex.getMessage(), request);
    }

    /** Decisão 2: erros de {@code @Valid} → 422 com a lista completa de campos. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponseDTO> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var bindingResult = ex.getBindingResult();
        List<ErrorResponseDTO.CampoErroDTO> erros = Stream.concat(
                bindingResult.getFieldErrors().stream()
                        .map(e -> new ErrorResponseDTO.CampoErroDTO(e.getField(), e.getDefaultMessage())),
                bindingResult.getGlobalErrors().stream()
                        .map(e -> new ErrorResponseDTO.CampoErroDTO(e.getObjectName(), e.getDefaultMessage()))
        ).toList();

        HttpStatus status = HttpStatus.UNPROCESSABLE_ENTITY;
        return ResponseEntity.status(status).body(ErrorResponseDTO.of(status, "ERR_VALIDACAO",
                "Erros de validação nos campos informados.", request.getRequestURI(), erros));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, RestAccessDeniedHandler.CODIGO, RestAccessDeniedHandler.MENSAGEM, request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenericException(Exception ex, HttpServletRequest request) {
        log.error("Erro não tratado em {} {}", request.getMethod(), request.getRequestURI(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "ERR_INTERNAL_SERVER_ERROR",
                "Ocorreu um erro interno no servidor. Entre em contato com a administração.", request);
    }

    private ResponseEntity<ErrorResponseDTO> build(HttpStatus status, String codigo, String mensagem,
                                                   HttpServletRequest request) {
        return ResponseEntity.status(status).body(ErrorResponseDTO.of(status, codigo, mensagem, request.getRequestURI()));
    }
}
