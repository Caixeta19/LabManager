package com.example.lab_manager.security;

import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.model.User;
import com.example.lab_manager.support.TestData;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtTokenProviderTest {

    private static final String SECRET = "segredo-de-teste-com-mais-de-32-caracteres-ok";

    @Test
    void tokenCarregaIdEmailERole() {
        User user = TestData.user(UserType.PROFESSOR);
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, 60_000);

        Claims claims = provider.parseClaims(provider.generateToken(user));

        assertThat(claims.getSubject()).isEqualTo(user.getId().toString());
        assertThat(claims.get(JwtTokenProvider.CLAIM_EMAIL, String.class)).isEqualTo(user.getEmail());
        assertThat(claims.get(JwtTokenProvider.CLAIM_ROLE, String.class)).isEqualTo("PROFESSOR");
    }

    @Test
    void tokenExpiradoLancaExpiredJwtException() {
        JwtTokenProvider provider = new JwtTokenProvider(SECRET, -1_000);
        String token = provider.generateToken(TestData.user(UserType.ALUNO));

        assertThatThrownBy(() -> provider.parseClaims(token)).isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void tokenAssinadoComOutraChaveEhInvalido() {
        String token = new JwtTokenProvider("outro-segredo-qualquer-com-mais-de-32-chars", 60_000)
                .generateToken(TestData.user(UserType.ALUNO));

        assertThatThrownBy(() -> new JwtTokenProvider(SECRET, 60_000).parseClaims(token))
                .isInstanceOf(JwtException.class)
                .isNotInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void segredoCurtoImpedeOStartup() {
        assertThatThrownBy(() -> new JwtTokenProvider("curto", 60_000))
                .isInstanceOf(IllegalStateException.class);
    }
}
