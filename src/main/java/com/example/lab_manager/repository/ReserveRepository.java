package com.example.lab_manager.repository;

import com.example.lab_manager.model.Reserve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface ReserveRepository extends JpaRepository<Reserve, UUID> {
    Optional<Reserve> findByTokenConfirmacao(String token);
    Optional<Reserve> findByCodigoQr(String codigoQr);

    @Query("SELECT COUNT(r) > 0 FROM Reserve r WHERE r.laboratory.id = :labId " +
            "AND r.status <> 'CANCELADA' AND r.status <> 'EXPIRADA' " +
            "AND ((r.dataHoraInicio < :fim) AND (r.dataHoraFim > :inicio))")
    boolean existsConflictingReserve(@Param("labId") UUID labId,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim);
}