package com.example.lab_manager.dto.response;

import com.example.lab_manager.enums.UserType;
import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        String registration,
        UserType type,
        Boolean active
) {}