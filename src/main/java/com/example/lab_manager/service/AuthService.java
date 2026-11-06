package com.example.lab_manager.service;

import com.example.lab_manager.config.LoginLockProperties;
import com.example.lab_manager.dto.request.LoginRequestDTO;
import com.example.lab_manager.dto.response.AuthResponseDTO;
import com.example.lab_manager.exceptions.ContaBloqueadaException;
import com.example.lab_manager.exceptions.CredenciaisInvalidasException;
import com.example.lab_manager.exceptions.LabManagerException;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.UserRepository;
import com.example.lab_manager.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    static final String MSG_CREDENCIAIS_INVALIDAS = "Usuário ou senha inválidos. Verifique seus dados.";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final LoginLockProperties lockProperties;
    private final Clock clock;

    /**
     * Login com bloqueio de conta (decisão 3). O {@code noRollbackFor} garante que o contador
     * de tentativas seja gravado mesmo quando o login falha com exceção.
     */
    @Transactional(noRollbackFor = LabManagerException.class)
    public AuthResponseDTO login(LoginRequestDTO dto) {
        User user = userRepository.findByEmailIgnoreCase(dto.email().trim())
                .orElseThrow(() -> new CredenciaisInvalidasException(MSG_CREDENCIAIS_INVALIDAS));

        LocalDateTime agora = LocalDateTime.now(clock);

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(agora)) {
            throw new ContaBloqueadaException();
        }

        if (!user.isActive()) {
            throw new CredenciaisInvalidasException(MSG_CREDENCIAIS_INVALIDAS);
        }

        if (!passwordEncoder.matches(dto.senha(), user.getPassword())) {
            registrarFalha(user, agora);
            throw new CredenciaisInvalidasException(MSG_CREDENCIAIS_INVALIDAS);
        }

        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(agora);
        userRepository.save(user);

        return new AuthResponseDTO(
                tokenProvider.generateToken(user),
                user.getId(),
                user.getEmail(),
                user.getType().name()
        );
    }

    private void registrarFalha(User user, LocalDateTime agora) {
        int tentativas = user.getFailedAttempts() + 1;
        if (tentativas >= lockProperties.maxAttempts()) {
            user.setFailedAttempts(0);
            user.setLockedUntil(agora.plus(lockProperties.lockDuration()));
        } else {
            user.setFailedAttempts(tentativas);
        }
        userRepository.save(user);
    }
}
