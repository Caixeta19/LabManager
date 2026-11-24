package com.example.lab_manager.model;

import java.security.SecureRandom;
import java.util.Base64;

/** Tokens do QR fixo de check-in do laboratório: 32 bytes aleatórios em Base64 URL (43 caracteres). */
public final class CheckinTokens {

    private static final SecureRandom RANDOM = new SecureRandom();

    private CheckinTokens() {}

    public static String novo() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
