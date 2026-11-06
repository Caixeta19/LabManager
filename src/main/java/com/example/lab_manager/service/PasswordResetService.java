package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.ResetPasswordRequestDTO;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.PasswordResetToken;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.PasswordResetTokenRepository;
import com.example.lab_manager.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;

/**
 * "Esqueci minha senha". O token enviado por e-mail nunca é gravado, só o hash SHA-256.
 * <p>
 * O e-mail sai em outra thread: assim a resposta leva o mesmo tempo com ou sem conta cadastrada,
 * e o tempo de resposta não revela quais e-mails existem.
 */
@Slf4j
@Service
public class PasswordResetService {

    static final Duration VALIDADE = Duration.ofMinutes(30);
    static final String MSG_LINK_INVALIDO = "Link inválido ou expirado.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;
    private final Clock clock;
    private final TaskExecutor mailExecutor;
    private final boolean logarLink;

    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                PasswordEncoder passwordEncoder,
                                MailService mailService,
                                Clock clock,
                                @Qualifier("applicationTaskExecutor") TaskExecutor mailExecutor,
                                @Value("${app.mail.log-reset-link:false}") boolean logarLink) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.mailService = mailService;
        this.clock = clock;
        this.mailExecutor = mailExecutor;
        this.logarLink = logarLink;
    }

    /** Nunca informa se o e-mail existe: quem chama sempre responde 204. */
    @Transactional
    public void solicitar(String email) {
        User user = userRepository.findByEmailIgnoreCase(email.trim())
                .filter(User::isActive)
                .orElse(null);
        if (user == null) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now(clock);
        tokenRepository.invalidarPendentes(user.getId(), agora);

        String token = UUID.randomUUID().toString();
        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .tokenHash(hash(token))
                .expiresAt(agora.plus(VALIDADE))
                .build());

        String nome = user.getName();
        String destino = user.getEmail();
        try {
            mailExecutor.execute(() -> enviar(nome, destino, token));
        } catch (RuntimeException e) {
            log.warn("Não foi possível agendar o e-mail de redefinição de senha: {}", e.getMessage());
        }
    }

    @Transactional
    public void redefinir(ResetPasswordRequestDTO dto) {
        LocalDateTime agora = LocalDateTime.now(clock);

        PasswordResetToken token = tokenRepository.findByTokenHashForUpdate(hash(dto.token()))
                .filter(t -> t.getUsedAt() == null)
                .filter(t -> t.getExpiresAt().isAfter(agora))
                .filter(t -> t.getUser().isActive())
                .orElseThrow(() -> new RegraDeNegocioException(MSG_LINK_INVALIDO, "ERR_AUTH_002"));

        User user = token.getUser();
        user.setPassword(passwordEncoder.encode(dto.novaSenha()));
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        token.setUsedAt(agora);
        tokenRepository.save(token);
    }

    private void enviar(String nome, String destino, String token) {
        try {
            mailService.enviarRedefinicaoSenha(nome, destino, token);
        } catch (Exception e) {
            log.warn("Falha ao enviar o e-mail de redefinição de senha: {}", e.getMessage());
            if (logarLink) {
                log.info("Link de redefinição (app.mail.log-reset-link): {}", mailService.linkRedefinicaoSenha(token));
            }
        }
    }

    static String hash(String token) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível.", e);
        }
    }
}
