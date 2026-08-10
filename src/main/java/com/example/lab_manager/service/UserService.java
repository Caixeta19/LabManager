package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.UserRequestDTO;
import com.example.lab_manager.dto.response.UserResponseDTO;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO criar(UserRequestDTO dto) {
        if (userRepository.existsByEmail(dto.email())) {
            throw new RegraDeNegocioException("Já existe um usuário com esse e-mail.");
        }

        User user = User.builder()
                .name(dto.name())
                .email(dto.email())
                .password(passwordEncoder.encode(dto.password()))
                .registration(dto.registration())
                .type(dto.type())
                .build();

        return UserResponseDTO.fromEntity(userRepository.save(user));
    }

    @Transactional(readOnly = true)
    public UserResponseDTO buscarPorId(UUID id) {
        return UserResponseDTO.fromEntity(buscarEntidadePorId(id));
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> listarTodos() {
        return userRepository.findAll().stream()
                .map(UserResponseDTO::fromEntity)
                .toList();
    }

    @Transactional
    public void desativar(UUID id) {
        User user = buscarEntidadePorId(id);
        user.setActive(false);
        userRepository.save(user);
    }

    User buscarEntidadePorId(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado: " + id));
    }
}