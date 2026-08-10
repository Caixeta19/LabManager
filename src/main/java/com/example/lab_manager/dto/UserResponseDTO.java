package com.example.lab_manager.dto;

import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.model.User;

import java.util.UUID;

public record UserResponseDTO(
        UUID id,
        String name,
        String email,
        String registration,
        UserType type,
        boolean active
) {
    public static UserResponseDTO fromEntity(User user) {
        return new UserResponseDTO(
                user.getId(), user.getName(), user.getEmail(), user.getRegistration(), user.getType(), user.isActive()
        );
    }



}
