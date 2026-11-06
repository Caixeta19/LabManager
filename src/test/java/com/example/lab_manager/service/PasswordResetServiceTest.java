package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.ResetPasswordRequestDTO;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.PasswordResetToken;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.PasswordResetTokenRepository;
import com.example.lab_manager.repository.UserRepository;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailSendException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;

import static com.example.lab_manager.support.TestData.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String TOKEN = "11111111-2222-3333-4444-555555555555";

    @Mock UserRepository userRepository;
    @Mock PasswordResetTokenRepository tokenRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock MailService mailService;

    PasswordResetService service;
    User user;

    @BeforeEach
    void setUp() {
        service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, mailService,
                TestData.CLOCK, new SyncTaskExecutor(), false);
        user = TestData.user(UserType.ALUNO);
    }

    @Nested
    class Solicitacao {

        @Test
        void emailInexistenteNaoFazNadaENaoLanca() {
            when(userRepository.findByEmailIgnoreCase("ninguem@lab.com")).thenReturn(Optional.empty());

            assertThatCode(() -> service.solicitar("ninguem@lab.com")).doesNotThrowAnyException();

            verifyNoInteractions(tokenRepository, mailService);
        }

        @Test
        void usuarioInativoNaoRecebeLink() {
            user.setActive(false);
            when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

            service.solicitar(user.getEmail());

            verifyNoInteractions(tokenRepository, mailService);
        }

        @Test
        void gravaSoOHashComValidadeDe30MinutosEInvalidaOsAnteriores() {
            when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

            service.solicitar(user.getEmail());

            ArgumentCaptor<String> tokenEnviado = ArgumentCaptor.forClass(String.class);
            verify(mailService).enviarRedefinicaoSenha(eq(user.getName()), eq(user.getEmail()), tokenEnviado.capture());

            ArgumentCaptor<PasswordResetToken> salvo = ArgumentCaptor.forClass(PasswordResetToken.class);
            var ordem = inOrder(tokenRepository);
            ordem.verify(tokenRepository).invalidarPendentes(user.getId(), AGORA);
            ordem.verify(tokenRepository).save(salvo.capture());

            assertThat(salvo.getValue().getTokenHash())
                    .hasSize(64)
                    .isEqualTo(PasswordResetService.hash(tokenEnviado.getValue()))
                    .isNotEqualTo(tokenEnviado.getValue());
            assertThat(salvo.getValue().getExpiresAt()).isEqualTo(AGORA.plusMinutes(30));
            assertThat(salvo.getValue().getUsedAt()).isNull();
        }

        @Test
        void smtpFalhandoNaoPropagaAExcecao() {
            when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
            doThrow(new MailSendException("SMTP fora do ar"))
                    .when(mailService).enviarRedefinicaoSenha(anyString(), anyString(), anyString());

            assertThatCode(() -> service.solicitar(user.getEmail())).doesNotThrowAnyException();
        }

        @Test
        void filaDeEnvioCheiaNaoPropagaAExcecao() {
            service = new PasswordResetService(userRepository, tokenRepository, passwordEncoder, mailService,
                    TestData.CLOCK, tarefa -> { throw new TaskRejectedException("fila cheia"); }, false);
            when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));

            assertThatCode(() -> service.solicitar(user.getEmail())).doesNotThrowAnyException();
        }

        @Test
        void soEscreveOLinkNoLogQuandoConfigurado() {
            when(userRepository.findByEmailIgnoreCase(user.getEmail())).thenReturn(Optional.of(user));
            doThrow(new MailSendException("SMTP fora do ar"))
                    .when(mailService).enviarRedefinicaoSenha(anyString(), anyString(), anyString());

            service.solicitar(user.getEmail());

            verify(mailService, never()).linkRedefinicaoSenha(anyString());
        }
    }

    @Nested
    class Redefinicao {

        private final ResetPasswordRequestDTO dto = new ResetPasswordRequestDTO(TOKEN, "NovaSenha@1");

        private PasswordResetToken token(LocalDateTime expiraEm, LocalDateTime usadoEm) {
            PasswordResetToken token = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(PasswordResetService.hash(TOKEN))
                    .expiresAt(expiraEm)
                    .usedAt(usadoEm)
                    .build();
            when(tokenRepository.findByTokenHashForUpdate(PasswordResetService.hash(TOKEN)))
                    .thenReturn(Optional.of(token));
            return token;
        }

        private void assertLinkInvalido() {
            assertThatThrownBy(() -> service.redefinir(dto))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .hasMessage("Link inválido ou expirado.")
                    .extracting("codigoInterno", "httpStatus")
                    .containsExactly("ERR_AUTH_002", HttpStatus.BAD_REQUEST);
            verify(userRepository, never()).save(any());
        }

        @Test
        void trocaASenhaMarcaOTokenComoUsadoEDesbloqueiaAConta() {
            PasswordResetToken token = token(AGORA.plusMinutes(1), null);
            user.setFailedAttempts(4);
            user.setLockedUntil(AGORA.plusMinutes(10));
            when(passwordEncoder.encode("NovaSenha@1")).thenReturn("hash-novo");

            service.redefinir(dto);

            assertThat(user.getPassword()).isEqualTo("hash-novo");
            assertThat(user.getFailedAttempts()).isZero();
            assertThat(user.getLockedUntil()).isNull();
            assertThat(token.getUsedAt()).isEqualTo(AGORA);
        }

        @Test
        void tokenInexistente() {
            when(tokenRepository.findByTokenHashForUpdate(anyString())).thenReturn(Optional.empty());

            assertLinkInvalido();
        }

        @Test
        void tokenExpirado() {
            token(AGORA.minusSeconds(1), null);

            assertLinkInvalido();
        }

        @Test
        void tokenNoInstanteExatoDaExpiracaoEstaExpirado() {
            token(AGORA, null);

            assertLinkInvalido();
        }

        @Test
        void tokenJaUsado() {
            token(AGORA.plusMinutes(10), AGORA.minusMinutes(1));

            assertLinkInvalido();
        }

        @Test
        void usuarioDesativadoDepoisDoPedido() {
            token(AGORA.plusMinutes(10), null);
            user.setActive(false);

            assertLinkInvalido();
        }
    }
}
