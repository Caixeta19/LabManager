package com.example.lab_manager.service;

import com.example.lab_manager.config.LoginLockProperties;
import com.example.lab_manager.dto.request.LoginRequestDTO;
import com.example.lab_manager.dto.response.AuthResponseDTO;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.exceptions.ContaBloqueadaException;
import com.example.lab_manager.exceptions.CredenciaisInvalidasException;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.UserRepository;
import com.example.lab_manager.security.JwtTokenProvider;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static com.example.lab_manager.support.TestData.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/** Bloqueio de conta (decisão 3): 5 erros seguidos → 15 min bloqueado → 423 ERR_AUTH_LOCKED. */
@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    private static final String SENHA = "senha-correta";

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtTokenProvider tokenProvider;

    AuthService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new AuthService(userRepository, passwordEncoder, tokenProvider,
                new LoginLockProperties(5, Duration.ofMinutes(15)), TestData.CLOCK);
        user = TestData.user(UserType.ALUNO);
        lenient().when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
        lenient().when(passwordEncoder.matches(SENHA, user.getPassword())).thenReturn(true);
        lenient().when(passwordEncoder.matches("errada", user.getPassword())).thenReturn(false);
    }

    private LoginRequestDTO login(String senha) {
        return new LoginRequestDTO(user.getEmail(), senha);
    }

    @Test
    void loginCorretoRetornaTokenEZeraContador() {
        user.setFailedAttempts(3);
        when(tokenProvider.generateToken(user)).thenReturn("jwt");

        AuthResponseDTO dto = service.login(login(SENHA));

        assertThat(dto.token()).isEqualTo("jwt");
        assertThat(dto.role()).isEqualTo("ALUNO");
        assertThat(user.getFailedAttempts()).isZero();
        assertThat(user.getLockedUntil()).isNull();
        assertThat(user.getLastLoginAt()).isEqualTo(AGORA);
    }

    @Test
    void loginErradoNaoAtualizaOUltimoAcesso() {
        assertThatThrownBy(() -> service.login(login("errada")))
                .isInstanceOf(CredenciaisInvalidasException.class);

        assertThat(user.getLastLoginAt()).isNull();
    }

    @Test
    void senhaErradaIncrementaContador() {
        assertThatThrownBy(() -> service.login(login("errada")))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .extracting("codigoInterno").isEqualTo("ERR_AUTH_001");
        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
        verify(userRepository).save(user);
    }

    @Test
    void quintaTentativaErradaBloqueiaPor15Minutos() {
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.login(login("errada"))).isInstanceOf(CredenciaisInvalidasException.class);
        }
        assertThat(user.getLockedUntil()).isNull();

        assertThatThrownBy(() -> service.login(login("errada"))).isInstanceOf(CredenciaisInvalidasException.class);

        assertThat(user.getLockedUntil()).isEqualTo(AGORA.plusMinutes(15));
        assertThat(user.getFailedAttempts()).isZero();
    }

    @Test
    void contaBloqueadaRecusaAteSenhaCertaCom423() {
        user.setLockedUntil(AGORA.plusMinutes(1));

        assertThatThrownBy(() -> service.login(login(SENHA)))
                .isInstanceOf(ContaBloqueadaException.class)
                .extracting("codigoInterno", "httpStatus")
                .containsExactly("ERR_AUTH_LOCKED", HttpStatus.LOCKED);
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(tokenProvider, never()).generateToken(any());
    }

    @Test
    void depoisDoBloqueioOLoginVoltaAFuncionar() {
        user.setLockedUntil(AGORA.minusSeconds(1));
        when(tokenProvider.generateToken(user)).thenReturn("jwt");

        assertThat(service.login(login(SENHA)).token()).isEqualTo("jwt");
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void acertoNoMeioZeraASequencia() {
        when(tokenProvider.generateToken(user)).thenReturn("jwt");
        for (int i = 0; i < 4; i++) {
            assertThatThrownBy(() -> service.login(login("errada"))).isInstanceOf(CredenciaisInvalidasException.class);
        }
        service.login(login(SENHA));
        assertThatThrownBy(() -> service.login(login("errada"))).isInstanceOf(CredenciaisInvalidasException.class);

        assertThat(user.getFailedAttempts()).isEqualTo(1);
        assertThat(user.getLockedUntil()).isNull();
    }

    @Test
    void emailInexistenteRetornaMensagemGenerica() {
        when(userRepository.findByEmailIgnoreCase("nao@existe.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.login(new LoginRequestDTO("nao@existe.com", SENHA)))
                .isInstanceOf(CredenciaisInvalidasException.class)
                .hasMessage("Usuário ou senha inválidos. Verifique seus dados.");
    }

    @Test
    void usuarioInativoNaoLoga() {
        user.setActive(false);

        assertThatThrownBy(() -> service.login(login(SENHA))).isInstanceOf(CredenciaisInvalidasException.class);
        verify(tokenProvider, never()).generateToken(any());
    }
}
