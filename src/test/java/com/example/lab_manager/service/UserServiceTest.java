package com.example.lab_manager.service;

import com.example.lab_manager.dto.request.RegisterRequestDTO;
import com.example.lab_manager.dto.request.UserUpdateRequestDTO;
import com.example.lab_manager.dto.response.UserResponseDTO;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.exceptions.RecursoNaoEncontradoException;
import com.example.lab_manager.exceptions.RegraDeNegocioException;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.UserRepository;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static com.example.lab_manager.support.TestData.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock CurrentUserService currentUserService;

    UserService service;

    @BeforeEach
    void setUp() {
        service = new UserService(userRepository, passwordEncoder, currentUserService);
    }

    @Nested
    class Autocadastro {

        private final RegisterRequestDTO dto =
                new RegisterRequestDTO(" Ana Silva ", " ANA@Lab.com ", "Senha@123", " 2026001 ");

        @Test
        void criaSempreAlunoComEmailEmMinusculasESenhaEmHash() {
            when(passwordEncoder.encode("Senha@123")).thenReturn("hash");
            when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

            UserResponseDTO criado = service.register(dto);

            ArgumentCaptor<User> salvo = ArgumentCaptor.forClass(User.class);
            verify(userRepository).save(salvo.capture());
            assertThat(salvo.getValue().getType()).isEqualTo(UserType.ALUNO);
            assertThat(salvo.getValue().getPassword()).isEqualTo("hash");
            assertThat(criado.type()).isEqualTo(UserType.ALUNO);
            assertThat(criado.email()).isEqualTo("ana@lab.com");
            assertThat(criado.name()).isEqualTo("Ana Silva");
            assertThat(criado.registration()).isEqualTo("2026001");
            assertThat(criado.active()).isTrue();
        }

        @Test
        void emailDuplicadoLanca409() {
            when(userRepository.existsByEmailIgnoreCase("ANA@Lab.com")).thenReturn(true);

            assertThatThrownBy(() -> service.register(dto))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .extracting("codigoInterno", "httpStatus")
                    .containsExactly("ERR_USER_002", HttpStatus.CONFLICT);
            verify(userRepository, never()).save(any());
        }

        @Test
        void matriculaDuplicadaLanca409() {
            when(userRepository.existsByRegistration("2026001")).thenReturn(true);

            assertThatThrownBy(() -> service.register(dto))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .extracting("codigoInterno", "httpStatus")
                    .containsExactly("ERR_USER_001", HttpStatus.CONFLICT);
            verify(userRepository, never()).save(any());
        }
    }

    @Nested
    class Edicao {

        private User user;

        @BeforeEach
        void setUp() {
            user = TestData.user(UserType.ALUNO);
            lenient().when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
            lenient().when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));
        }

        private UserUpdateRequestDTO dto(String email, boolean active, String senha) {
            return new UserUpdateRequestDTO("Novo Nome", email, "MAT-1", UserType.PROFESSOR, active, senha);
        }

        @Test
        void atualizaOsDadosEMantemASenhaQuandoNaoInformada() {
            String senhaAtual = user.getPassword();

            UserResponseDTO atualizado = service.update(user.getId(), dto("NOVO@lab.com", true, null));

            assertThat(atualizado.name()).isEqualTo("Novo Nome");
            assertThat(atualizado.email()).isEqualTo("novo@lab.com");
            assertThat(atualizado.type()).isEqualTo(UserType.PROFESSOR);
            assertThat(user.getPassword()).isEqualTo(senhaAtual);
            verify(passwordEncoder, never()).encode(anyString());
        }

        @Test
        void trocaASenhaEDesbloqueiaAConta() {
            user.setFailedAttempts(3);
            user.setLockedUntil(AGORA.plusMinutes(10));
            when(passwordEncoder.encode("NovaSenha@1")).thenReturn("hash-novo");

            service.update(user.getId(), dto("novo@lab.com", true, "NovaSenha@1"));

            assertThat(user.getPassword()).isEqualTo("hash-novo");
            assertThat(user.getFailedAttempts()).isZero();
            assertThat(user.getLockedUntil()).isNull();
        }

        @Test
        void reativaUsuarioInativo() {
            user.setActive(false);

            assertThat(service.update(user.getId(), dto("novo@lab.com", true, null)).active()).isTrue();
        }

        @Test
        void emailDeOutroUsuarioLanca409() {
            when(userRepository.existsByEmailIgnoreCaseAndIdNot("outro@lab.com", user.getId())).thenReturn(true);

            assertThatThrownBy(() -> service.update(user.getId(), dto("Outro@lab.com", true, null)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .extracting("codigoInterno", "httpStatus")
                    .containsExactly("ERR_USER_002", HttpStatus.CONFLICT);
            verify(userRepository, never()).save(any());
        }

        @Test
        void matriculaDeOutroUsuarioLanca409() {
            when(userRepository.existsByRegistrationAndIdNot("MAT-1", user.getId())).thenReturn(true);

            assertThatThrownBy(() -> service.update(user.getId(), dto("novo@lab.com", true, null)))
                    .isInstanceOf(RegraDeNegocioException.class)
                    .extracting("codigoInterno", "httpStatus")
                    .containsExactly("ERR_USER_001", HttpStatus.CONFLICT);
        }

        @Test
        void usuarioInexistenteLanca404() {
            UUID id = UUID.randomUUID();
            when(userRepository.findById(id)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.update(id, dto("novo@lab.com", true, null)))
                    .isInstanceOf(RecursoNaoEncontradoException.class);
        }
    }

    @Test
    void findCurrentDevolveOUsuarioDoToken() {
        User user = TestData.user(UserType.PROFESSOR);
        user.setLastLoginAt(AGORA);
        when(currentUserService.getUsuarioAutenticado()).thenReturn(user);

        UserResponseDTO dto = service.findCurrent();

        assertThat(dto.id()).isEqualTo(user.getId());
        assertThat(dto.lastLoginAt()).isEqualTo(AGORA);
    }
}
