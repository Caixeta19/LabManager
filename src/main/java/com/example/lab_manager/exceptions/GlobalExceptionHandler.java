package com.example.lab_manager.exceptions;

import com.example.lab_manager.dto.error.ErrorResponseDTO;
import com.example.lab_manager.dto.error.ValidationErrorResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    public ResponseEntity<ErrorResponseDTO> handleRecursoNaoEncontrado(
            RecursoNaoEncontradoException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(HorarioConflitanteException.class)
    public ResponseEntity<ErrorResponseDTO> handleHorarioConflitante(
            HorarioConflitanteException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ReservaNaoConfirmadaException.class)
    public ResponseEntity<ErrorResponseDTO> handleReservaNaoConfirmada(
            ReservaNaoConfirmadaException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(RegraDeNegocioException.class)
    public ResponseEntity<ErrorResponseDTO> handleRegraDeNegocio(
            RegraDeNegocioException ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage(), request);
    }

    // Erros de validação do Bean Validation (@NotBlank, @Email, @Future, etc.)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponseDTO> handleValidacao(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        List<ValidationErrorResponseDTO.CampoErro> erros = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationErrorResponseDTO.CampoErro(fe.getField(), fe.getDefaultMessage()))
                .toList();

        ValidationErrorResponseDTO body = new ValidationErrorResponseDTO(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                erros,
                request.getRequestURI()
        );

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // Fallback para qualquer exceção não mapeada — evita vazar stacktrace pro cliente
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponseDTO> handleGenerico(
            Exception ex, HttpServletRequest request) {
        return construirResposta(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno no servidor.", request);
    }

    private ResponseEntity<ErrorResponseDTO> construirResposta(
            HttpStatus status, String mensagem, HttpServletRequest request) {

        ErrorResponseDTO body = new ErrorResponseDTO(
                LocalDateTime.now(),
                status.value(),
                status.getReasonPhrase(),
                mensagem,
                request.getRequestURI()
        );

        return ResponseEntity.status(status).body(body);
    }
}
