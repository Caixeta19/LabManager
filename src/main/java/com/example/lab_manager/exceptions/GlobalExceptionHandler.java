package com.example.lab_manager.exceptions;

import com.example.lab_manager.dto.error.ErrorResponseDTO;
import com.example.lab_manager.security.RestAccessDeniedHandler;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.stream.Stream;

/**
 * Traduz exceções para o corpo padrão (decisão 13). Nenhuma resposta expõe stack trace,
 * nome de classe ou mensagem do Hibernate/JDBC: o detalhe técnico vai só para o log.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Exceções de domínio: cada uma já carrega status e código. */
    @ExceptionHandler(LabManagerException.class)
    public ResponseEntity<ErrorResponseDTO> handleLabManagerException(LabManagerException ex, HttpServletRequest request) {
        if (ex.getHttpStatus().is5xxServerError()) {
            log.error("Erro de domínio {} em {} {}", ex.getCodigoInterno(), request.getMethod(), request.getRequestURI(), ex);
        }
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

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponseDTO> handleNotReadable(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "ERR_REQUISICAO_INVALIDA",
                "Corpo da requisição ausente, malformado ou com valores em formato inválido.", request);
    }

    /** Ex.: UUID inválido em {@code /reserves/{id}}. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponseDTO> handleTypeMismatch(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "ERR_PARAMETRO_INVALIDO",
                "O parâmetro '" + ex.getName() + "' tem formato inválido.", request);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponseDTO> handleMissingParam(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return build(HttpStatus.BAD_REQUEST, "ERR_PARAMETRO_INVALIDO",
                "O parâmetro '" + ex.getParameterName() + "' é obrigatório.", request);
    }

    /** Violação de constraint do banco (UNIQUE, FK...) que escapou das validações do service. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponseDTO> handleDataIntegrity(DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade em {} {}: {}", request.getMethod(), request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "ERR_CONFLITO",
                "A operação conflita com dados já existentes.", request);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponseDTO> handleNoResource(NoResourceFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "ERR_NOT_FOUND", "Recurso não encontrado.", request);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMethodNotSupported(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.METHOD_NOT_ALLOWED, "ERR_METODO_NAO_SUPORTADO",
                "Método HTTP " + ex.getMethod() + " não suportado para este recurso.", request);
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorResponseDTO> handleMediaType(HttpMediaTypeNotSupportedException ex, HttpServletRequest request) {
        return build(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "ERR_MIDIA_NAO_SUPORTADA",
                "Content-Type não suportado. Use application/json.", request);
    }

    /** {@code @PreAuthorize} negado dentro do controller → mesmo corpo do {@link RestAccessDeniedHandler}. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponseDTO> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        return build(HttpStatus.FORBIDDEN, RestAccessDeniedHandler.CODIGO, RestAccessDeniedHandler.MENSAGEM, request);
    }

    /** 500 genérico: sem stack trace nem mensagem interna na resposta. */
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
