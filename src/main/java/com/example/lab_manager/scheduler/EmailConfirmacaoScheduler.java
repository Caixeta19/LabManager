package com.example.lab_manager.scheduler;

import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.repository.ReserveRepository;
import com.example.lab_manager.service.MailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * RF21 / RN15 / RN16: a cada 60s, envia o e-mail de confirmação das reservas PENDENTE
 * que começam entre agora+55min e agora+65min e ainda não receberam o e-mail.
 * <p>
 * Cada reserva é processada isoladamente: se o SMTP falhar, registra WARN, mantém
 * {@code emailConfirmacaoEnviado = false} (nova tentativa no próximo ciclo) e segue para a próxima.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailConfirmacaoScheduler {

    static final Duration INICIO_JANELA = Duration.ofMinutes(55);
    static final Duration FIM_JANELA = Duration.ofMinutes(65);

    private final ReserveRepository reserveRepository;
    private final MailService mailService;
    private final Clock clock;

    @Scheduled(fixedDelayString = "${app.jobs.email-confirmacao.fixed-delay-ms:60000}")
    public void enviarConfirmacoes() {
        LocalDateTime agora = LocalDateTime.now(clock);
        List<Reserve> reservas = reserveRepository.findParaEnvioDeConfirmacao(
                ReserveStatus.PENDENTE, agora.plus(INICIO_JANELA), agora.plus(FIM_JANELA));

        for (Reserve reserve : reservas) {
            try {
                mailService.enviarConfirmacao(reserve);
                reserveRepository.marcarEmailConfirmacaoEnviado(reserve.getId());
                log.info("E-mail de confirmação enviado: reserva={}", reserve.getId());
            } catch (Exception e) {
                log.warn("Falha ao enviar e-mail de confirmação: reserva={} motivo={}. Nova tentativa no próximo ciclo.",
                        reserve.getId(), e.getMessage());
            }
        }
    }
}
