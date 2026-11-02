package com.example.lab_manager.scheduler;

import com.example.lab_manager.LabManagerApplication;
import com.example.lab_manager.enums.LoanStatus;
import com.example.lab_manager.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * RF20 / RF22 / RN24: de hora em hora, marca como ATRASADO (em lote) os empréstimos ATIVO
 * com {@code dataDevolucaoPrevista < hoje} e sem devolução. A devolução vale até o fim do dia previsto.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LoanAtrasadoScheduler {

    private final LoanRepository loanRepository;
    private final Clock clock;

    @Scheduled(cron = "${app.jobs.loan-atrasado.cron:0 0 * * * *}", zone = LabManagerApplication.TIMEZONE)
    public void marcarAtrasados() {
        int marcados = loanRepository.marcarAtrasados(LoanStatus.ATIVO, LoanStatus.ATRASADO, LocalDate.now(clock));
        if (marcados > 0) {
            log.info("Empréstimos marcados como ATRASADO: {}", marcados);
        }
    }
}
