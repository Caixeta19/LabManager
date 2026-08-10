package com.example.lab_manager.dto.error;

import java.time.LocalDateTime;
import java.util.List;

public record ValidationErrorResponseDTO(
        LocalDateTime timestamp,
        int status,
        String erro,
        List<CampoErro> campos,
        String caminho
) {
    public record CampoErro(String campo, String mensagem){}
}
