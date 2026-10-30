package com.example.lab_manager.dto.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.LocalDateTime;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponseDTO(
        LocalDateTime timestamp,
        Integer status,
        String error,
        String codigoInterno,
        String message,
        String path,
        List<FieldErrorDTO> errors
) {
    public record FieldErrorDTO(
            String field,
            String message
    ) {}
}