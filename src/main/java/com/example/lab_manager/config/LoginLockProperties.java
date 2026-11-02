package com.example.lab_manager.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Bloqueio de conta (decisão 3): depois de {@code maxAttempts} tentativas erradas seguidas,
 * o login fica bloqueado por {@code lockDuration}.
 */
@ConfigurationProperties(prefix = "app.security.login")
public record LoginLockProperties(int maxAttempts, Duration lockDuration) {
    public LoginLockProperties {
        if (maxAttempts <= 0) maxAttempts = 5;
        if (lockDuration == null) lockDuration = Duration.ofMinutes(15);
    }
}
