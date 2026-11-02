package com.example.lab_manager.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

/** 401 com o corpo padrão: sem token, com token expirado ou com token inválido. */
@Component
@RequiredArgsConstructor
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final SecurityErrorWriter errorWriter;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        Object jwtError = request.getAttribute(JwtAuthenticationFilter.ATTR_JWT_ERROR);

        if (JwtAuthenticationFilter.ERR_JWT_EXPIRED.equals(jwtError)) {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    JwtAuthenticationFilter.ERR_JWT_EXPIRED, "Sessão expirada. Faça login novamente.");
        } else if (JwtAuthenticationFilter.ERR_JWT_MALFORMED.equals(jwtError)) {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    JwtAuthenticationFilter.ERR_JWT_MALFORMED, "Token de autenticação inválido.");
        } else {
            errorWriter.write(request, response, HttpStatus.UNAUTHORIZED,
                    "ERR_UNAUTHORIZED", "Autenticação necessária para acessar este recurso.");
        }
    }
}
