package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.RegisterRequestDTO;
import com.example.lab_manager.dto.request.UserRequestDTO;
import com.example.lab_manager.dto.request.UserUpdateRequestDTO;
import com.example.lab_manager.dto.response.UserResponseDTO;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;

    @Transactional
    public UserResponseDTO create(UserRequestDTO dto) {
        String email = dto.email().trim().toLowerCase(Locale.ROOT);

        // RN01 - e-mail único, sem diferenciar maiúsculas e minúsculas
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new RegraDeNegocioException("Já existe um usuário cadastrado com este e-mail.");
        }
        if (userRepository.existsByRegistration(dto.registration().trim())) {
            throw matriculaDuplicada();
        }

        User user = new User();
        user.setName(dto.name().trim());
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(dto.password())); // RN02
        user.setRegistration(dto.registration().trim());
        user.setType(dto.type());
        user.setActive(true);

        return toDTO(userRepository.save(user));
    }

    /** Autocadastro público: sempre ALUNO, com as mesmas validações do cadastro feito pelo ADMIN. */
    @Transactional
    public UserResponseDTO register(RegisterRequestDTO dto) {
        if (userRepository.existsByEmailIgnoreCase(dto.email().trim())) {
            throw emailDuplicado();
        }
        return create(new UserRequestDTO(dto.name(), dto.email(), dto.password(), dto.registration(), UserType.ALUNO));
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

    @Transactional(readOnly = true)
    public UserResponseDTO findCurrent() {
        return toDTO(currentUserService.getUsuarioAutenticado());
    }

    /** Edição pelo ADMIN. Também reativa o usuário; a senha só muda se vier preenchida. */
    @Transactional
    public UserResponseDTO update(UUID id, UserUpdateRequestDTO dto) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));

        String email = dto.email().trim().toLowerCase(Locale.ROOT);
        String registration = dto.registration().trim();

        if (userRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw emailDuplicado();
        }
        if (userRepository.existsByRegistrationAndIdNot(registration, id)) {
            throw matriculaDuplicada();
        }

        user.setName(dto.name().trim());
        user.setEmail(email);
        user.setRegistration(registration);
        user.setType(dto.type());
        user.setActive(dto.active());

        if (dto.password() != null) {
            user.setPassword(passwordEncoder.encode(dto.password())); // RN02
            user.setFailedAttempts(0);
            user.setLockedUntil(null);
        }

        return toDTO(userRepository.save(user));
    }

    @Transactional
    public void deactivate(UUID id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
        user.setActive(false); // RN03 - soft delete
        userRepository.save(user);
    }

    private static RegraDeNegocioException matriculaDuplicada() {
        return new RegraDeNegocioException("Já existe um usuário cadastrado com esta matrícula.",
                "ERR_USER_001", HttpStatus.CONFLICT);
    }

    private static RegraDeNegocioException emailDuplicado() {
        return new RegraDeNegocioException("Já existe um usuário cadastrado com este e-mail.",
                "ERR_USER_002", HttpStatus.CONFLICT);
    }

    private UserResponseDTO toDTO(User user) {
        return new UserResponseDTO(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRegistration(),
                user.getType(),
                user.isActive(),
                user.getLastLoginAt()
        );
    }
}
