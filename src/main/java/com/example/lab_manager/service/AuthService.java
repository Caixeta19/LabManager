package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.LoginRequestDTO;
import com.example.lab_manager.dto.response.AuthResponseDTO;
import com.example.lab_manager.exceptions.CredenciaisInvalidasException;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.UserRepository;
import com.example.lab_manager.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;
    private final UserRepository userRepository;

    public AuthResponseDTO login(LoginRequestDTO dto) {
        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.email(), dto.senha())
            );

            User user = userRepository.findByEmail(dto.email())
                    .orElseThrow(() -> new CredenciaisInvalidasException("Usuário ou senha inválidos. Verifique seus dados."));

            String token = tokenProvider.generateToken(authentication);

            return new AuthResponseDTO(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getRole().name()
            );

        } catch (Exception e) {
            throw new CredenciaisInvalidasException("Usuário ou senha inválidos. Verifique seus dados.");
        }
    }
}