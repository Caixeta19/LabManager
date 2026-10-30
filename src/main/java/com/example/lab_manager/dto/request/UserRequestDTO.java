package com.example.lab_manager.dto.request;

import com.example.lab_manager.enums.UserType; // Ou Role
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String nome,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 8, message = "A senha deve conter no mínimo 8 caracteres")
        String senha,

        @NotBlank(message = "A matrícula é obrigatória")
        String matricula,

        @NotNull(message = "O tipo/papel do usuário é obrigatório")
        UserType tipo
) {}