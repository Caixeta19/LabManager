package com.example.lab_manager.dto.response;

import java.util.UUID;

public record AuthResponseDTO(
        String token,
        UUID id,
        String email,
        String role
) {}