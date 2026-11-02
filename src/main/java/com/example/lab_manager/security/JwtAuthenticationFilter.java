package com.example.lab_manager.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Lê o header {@code Authorization: Bearer <jwt>} e preenche o SecurityContext.
 * Um token inválido não interrompe a cadeia: o motivo vai num atributo da requisição e,
 * se a rota exigir autenticação, o {@link RestAuthenticationEntryPoint} responde 401.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    public static final String ATTR_JWT_ERROR = "labmanager.jwt.error";
    public static final String ERR_JWT_EXPIRED = "ERR_JWT_EXPIRED";
    public static final String ERR_JWT_MALFORMED = "ERR_JWT_MALFORMED";

    private static final Logger SECURITY_LOG = LoggerFactory.getLogger("SECURITY");

    private final JwtTokenProvider tokenProvider;
    private final UserDetailsServiceImpl userDetailsService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String jwt = getJwtFromRequest(request);

        if (jwt != null) {
            try {
                Claims claims = tokenProvider.parseClaims(jwt);
                String email = claims.get(JwtTokenProvider.CLAIM_EMAIL, String.class);
                UserDetails userDetails = userDetailsService.loadUserByUsername(email);

                if (userDetails.isEnabled()) {
                    var authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            } catch (ExpiredJwtException ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute(ATTR_JWT_ERROR, ERR_JWT_EXPIRED);
            } catch (JwtException | IllegalArgumentException ex) {
                SecurityContextHolder.clearContext();
                request.setAttribute(ATTR_JWT_ERROR, ERR_JWT_MALFORMED);
                SECURITY_LOG.warn("{} ip={} path={} motivo={}", ERR_JWT_MALFORMED,
                        request.getRemoteAddr(), request.getRequestURI(), ex.getClass().getSimpleName());
            } catch (UsernameNotFoundException ex) {
                // token com assinatura válida, mas o usuário não existe mais
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
