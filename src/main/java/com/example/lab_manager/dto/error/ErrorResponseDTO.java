package com.example.lab_manager.dto.error;

import java.time.LocalDateTime;

public record ErrorResponseDTO(
        LocalDateTime timestap,
        int status,
        String erro,
        String mensagem,
        String caminho

) {
}
