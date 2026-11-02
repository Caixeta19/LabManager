package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.UserRequestDTO;
import com.example.lab_manager.dto.response.UserResponseDTO;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.email())) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este e-mail.");
        }

        User user = new User();
        user.setName(dto.nome());
        user.setEmail(dto.email());
        user.setPassword(passwordEncoder.encode(dto.senha()));
        user.setRegistration(dto.matricula());
        user.setType(dto.tipo());
        user.setActive(true);

        User saved = userRepository.save(user);
        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<UserResponseDTO> findAll() {
        return userRepository.findAll().stream().map(this::toDTO).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public UserResponseDTO findById(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        return toDTO(user);
    }

    @Transactional
    public void deactivate(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        user.setActive(false);
        userRepository.save(user);
    }

    private UserResponseDTO toDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRegistration(),
                user.getType(),
                user.isActive()
        );
    }
}