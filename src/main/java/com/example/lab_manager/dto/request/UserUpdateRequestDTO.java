package com.example.lab_manager.dto.request;

import com.example.lab_manager.enums.UserType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UserUpdateRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @NotBlank(message = "A matrícula é obrigatória")
        String registration,

        @NotNull(message = "O tipo/papel do usuário é obrigatório")
        UserType type,

        @NotNull(message = "Informe se o usuário está ativo")
        Boolean active,

        /** Opcional. Sem valor, a senha atual é mantida. */
        @Size(min = 8, max = 64, message = "A senha deve ter entre 8 e 64 caracteres")
        String password
) {}
