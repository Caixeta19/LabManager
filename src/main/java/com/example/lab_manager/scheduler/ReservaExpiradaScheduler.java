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
 * Decisão 1: marca como EXPIRADA toda reserva PENDENTE cujo {@code dataHoraInicio} já passou.
 * A transição passa por {@link ReserveStatus#podeTransicionarPara(ReserveStatus)}, como todas as outras.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ReservaExpiradaScheduler {

    private final ReserveRepository reserveRepository;
    private final Clock clock;

    @Transactional
    @Scheduled(fixedDelayString = "${app.jobs.reserva-expirada.fixed-delay-ms:60000}")
    public void expirarPendentes() {
        List<Reserve> vencidas = reserveRepository.findByStatusAndDataHoraInicioBefore(
                ReserveStatus.PENDENTE, LocalDateTime.now(clock));

        int expiradas = 0;
        for (Reserve reserve : vencidas) {
            if (reserve.getStatus().podeTransicionarPara(ReserveStatus.EXPIRADA)) {
                reserve.setStatus(ReserveStatus.EXPIRADA); // RN14: mantida no banco
                expiradas++;
            }
        }

        if (expiradas > 0) {
            reserveRepository.saveAll(vencidas);
            log.info("Reservas PENDENTE marcadas como EXPIRADA: {}", expiradas);
        }
    }
}
