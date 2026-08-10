package com.example.lab_manager.dto.request;

import com.example.lab_manager.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
        @NotBlank String name,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String registration,
        @NotNull UserType type)
{}
