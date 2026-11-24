package com.example.lab_manager.scheduler;

import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.repository.ReserveRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Marca como NAO_COMPARECEU toda reserva CONFIRMADA que já terminou ({@code dataHoraFim} passou) sem check-in.
 * A transição passa por {@link ReserveStatus#podeTransicionarPara(ReserveStatus)}, como todas as outras.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class NaoCompareceuScheduler {

    private final ReserveRepository reserveRepository;
    private final Clock clock;

    @Transactional
    @Scheduled(fixedDelayString = "${app.jobs.nao-compareceu.fixed-delay-ms:60000}")
    public void marcarNaoComparecimentos() {
        List<Reserve> encerradas = reserveRepository.findConfirmadasEncerradasSemCheckIn(LocalDateTime.now(clock));

        int marcadas = 0;
        for (Reserve reserve : encerradas) {
            if (reserve.getStatus().podeTransicionarPara(ReserveStatus.NAO_COMPARECEU)) {
                reserve.setStatus(ReserveStatus.NAO_COMPARECEU); // RN14: mantida no banco
                marcadas++;
            }
        }

        if (marcadas > 0) {
            reserveRepository.saveAll(encerradas);
            log.info("Reservas CONFIRMADA sem check-in marcadas como NAO_COMPARECEU: {}", marcadas);
        }
    }
}
