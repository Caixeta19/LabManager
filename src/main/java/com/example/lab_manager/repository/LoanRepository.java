package com.example.lab_manager.repository;

import com.example.lab_manager.enums.LoanStatus;
import com.example.lab_manager.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByUserIdOrderByDataRetiradaDesc(UUID userId);

    /**
     * RN24 / decisão 14: em lote, empréstimos {@code de} (ATIVO) com {@code dataDevolucaoPrevista < hoje}
     * e sem devolução passam para {@code para} (ATRASADO). Os já ATRASADO não entram no filtro.
     *
     * @return quantidade de empréstimos marcados
     */
    @Transactional
    @Modifying
    @Query("UPDATE Loan l SET l.status = :para " +
            "WHERE l.status = :de AND l.dataDevolucaoPrevista < :hoje AND l.dataDevolucaoReal IS NULL")
    int marcarAtrasados(@Param("de") LoanStatus de,
                        @Param("para") LoanStatus para,
                        @Param("hoje") LocalDate hoje);
}
