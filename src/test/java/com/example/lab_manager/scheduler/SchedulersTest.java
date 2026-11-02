package com.example.lab_manager.scheduler;

import com.example.lab_manager.enums.LoanStatus;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.model.User;
import com.example.lab_manager.repository.LoanRepository;
import com.example.lab_manager.repository.ReserveRepository;
import com.example.lab_manager.service.MailService;
import com.example.lab_manager.support.TestData;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;

import java.time.LocalDate;
import java.util.List;

import static com.example.lab_manager.support.TestData.AGORA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SchedulersTest {

    @Mock ReserveRepository reserveRepository;
    @Mock LoanRepository loanRepository;
    @Mock MailService mailService;

    private final User user = TestData.user(UserType.ALUNO);
    private final Laboratory lab = TestData.lab(true);

    private Reserve reserva(ReserveStatus status) {
        return TestData.reserve(user, lab, status, AGORA.plusHours(1), AGORA.plusHours(2));
    }

    @Nested
    class EmailConfirmacao {

        @Test
        void buscaPendentesNaJanelaDe55a65Minutos() {
            var job = new EmailConfirmacaoScheduler(reserveRepository, mailService, TestData.CLOCK);
            when(reserveRepository.findParaEnvioDeConfirmacao(any(), any(), any())).thenReturn(List.of());

            job.enviarConfirmacoes();

            verify(reserveRepository).findParaEnvioDeConfirmacao(
                    ReserveStatus.PENDENTE, AGORA.plusMinutes(55), AGORA.plusMinutes(65));
        }

        @Test
        void enviaEMarcaOFlag() {
            var job = new EmailConfirmacaoScheduler(reserveRepository, mailService, TestData.CLOCK);
            Reserve r = reserva(ReserveStatus.PENDENTE);
            when(reserveRepository.findParaEnvioDeConfirmacao(any(), any(), any())).thenReturn(List.of(r));

            job.enviarConfirmacoes();

            verify(mailService).enviarConfirmacao(r);
            verify(reserveRepository).marcarEmailConfirmacaoEnviado(r.getId());
        }

        @Test
        void smtpForaDoArNaoDerrubaOJobNemMarcaOFlag() {
            var job = new EmailConfirmacaoScheduler(reserveRepository, mailService, TestData.CLOCK);
            Reserve falha = reserva(ReserveStatus.PENDENTE);
            Reserve ok = reserva(ReserveStatus.PENDENTE);
            when(reserveRepository.findParaEnvioDeConfirmacao(any(), any(), any())).thenReturn(List.of(falha, ok));
            doThrow(new MailSendException("Connection refused")).when(mailService).enviarConfirmacao(falha);

            assertThatCode(job::enviarConfirmacoes).doesNotThrowAnyException();

            verify(reserveRepository, never()).marcarEmailConfirmacaoEnviado(falha.getId()); // continua false
            verify(mailService).enviarConfirmacao(ok);                                      // segue para a próxima
            verify(reserveRepository).marcarEmailConfirmacaoEnviado(ok.getId());
        }
    }

    @Nested
    class LoanAtrasado {

        @Test
        void marcaEmLoteAtivosVencidosAntesDeHoje() {
            var job = new LoanAtrasadoScheduler(loanRepository, TestData.CLOCK);
            when(loanRepository.marcarAtrasados(any(), any(), any())).thenReturn(2);

            job.marcarAtrasados();

            // "< hoje": a devolução vale até o fim do dia previsto (decisão 14); ATRASADO não é reprocessado (RN24)
            verify(loanRepository).marcarAtrasados(LoanStatus.ATIVO, LoanStatus.ATRASADO, LocalDate.from(AGORA));
        }
    }

    @Nested
    class ReservaExpirada {

        @Test
        void pendenteComInicioPassadoViraExpirada() {
            var job = new ReservaExpiradaScheduler(reserveRepository, TestData.CLOCK);
            Reserve r = reserva(ReserveStatus.PENDENTE);
            when(reserveRepository.findByStatusAndDataHoraInicioBefore(ReserveStatus.PENDENTE, AGORA)).thenReturn(List.of(r));

            job.expirarPendentes();

            assertThat(r.getStatus()).isEqualTo(ReserveStatus.EXPIRADA);
            verify(reserveRepository).saveAll(List.of(r));
        }

        @Test
        void semReservasVencidasNaoSalvaNada() {
            var job = new ReservaExpiradaScheduler(reserveRepository, TestData.CLOCK);
            when(reserveRepository.findByStatusAndDataHoraInicioBefore(ReserveStatus.PENDENTE, AGORA)).thenReturn(List.of());

            job.expirarPendentes();

            verify(reserveRepository, never()).saveAll(any());
        }
    }
}
