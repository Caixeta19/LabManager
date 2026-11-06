package com.example.lab_manager.repository;

import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.model.Reserve;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface ReserveRepository extends JpaRepository<Reserve, UUID> {

    /** RN11: reservas nestes status não bloqueiam o horário. */
    Set<ReserveStatus> STATUS_IGNORADOS_NO_CONFLITO = EnumSet.of(ReserveStatus.CANCELADA, ReserveStatus.EXPIRADA);

    Optional<Reserve> findByTokenConfirmacao(String token);

    Optional<Reserve> findByCodigoQr(String codigoQr);

    @Query("SELECT COUNT(r) > 0 FROM Reserve r " +
            "WHERE r.laboratory.id = :labId " +
            "AND r.status NOT IN :statusIgnorados " +
            "AND r.dataHoraInicio < :fim AND r.dataHoraFim > :inicio")
    boolean existsConflictingReserve(@Param("labId") UUID labId,
                                     @Param("inicio") LocalDateTime inicio,
                                     @Param("fim") LocalDateTime fim,
                                     @Param("statusIgnorados") Collection<ReserveStatus> statusIgnorados);

    /** Reservas que começam em [de, ate), exceto as dos status informados. */
    @Query("SELECT COUNT(r) FROM Reserve r " +
            "WHERE r.dataHoraInicio >= :de AND r.dataHoraInicio < :ate AND r.status NOT IN :statusIgnorados")
    long countComInicioEntre(@Param("de") LocalDateTime de,
                             @Param("ate") LocalDateTime ate,
                             @Param("statusIgnorados") Collection<ReserveStatus> statusIgnorados);

    /** Laboratórios com reserva no status informado cobrindo o instante {@code agora}. */
    @Query("SELECT DISTINCT r.laboratory.id FROM Reserve r " +
            "WHERE r.status = :status AND r.dataHoraInicio <= :agora AND r.dataHoraFim > :agora")
    Set<UUID> findLaboratoriosEmUso(@Param("status") ReserveStatus status, @Param("agora") LocalDateTime agora);

    /** RF21/RN15: reservas que devem receber o e-mail de confirmação agora (início na janela informada). */
    @Query("SELECT r FROM Reserve r JOIN FETCH r.user JOIN FETCH r.laboratory " +
            "WHERE r.status = :status AND r.emailConfirmacaoEnviado = false " +
            "AND r.dataHoraInicio BETWEEN :de AND :ate")
    List<Reserve> findParaEnvioDeConfirmacao(@Param("status") ReserveStatus status,
                                             @Param("de") LocalDateTime de,
                                             @Param("ate") LocalDateTime ate);

    /** RN16: marca o envio. Retorna 0 se outra execução já marcou. */
    @Transactional
    @Modifying
    @Query("UPDATE Reserve r SET r.emailConfirmacaoEnviado = true " +
            "WHERE r.id = :id AND r.emailConfirmacaoEnviado = false")
    int marcarEmailConfirmacaoEnviado(@Param("id") UUID id);

    /** Decisão 1: reservas ainda PENDENTE cujo início já passou. */
    List<Reserve> findByStatusAndDataHoraInicioBefore(ReserveStatus status, LocalDateTime agora);
}
