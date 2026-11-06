package com.example.lab_manager.repository;

import com.example.lab_manager.model.PasswordResetToken;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, UUID> {

    /** Trava a linha para que dois envios simultâneos do mesmo link não troquem a senha duas vezes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM PasswordResetToken t JOIN FETCH t.user WHERE t.tokenHash = :hash")
    Optional<PasswordResetToken> findByTokenHashForUpdate(@Param("hash") String tokenHash);

    /** Invalida os tokens ainda não usados do usuário: só o link mais recente vale. */
    @Modifying
    @Query("UPDATE PasswordResetToken t SET t.usedAt = :agora WHERE t.user.id = :userId AND t.usedAt IS NULL")
    int invalidarPendentes(@Param("userId") UUID userId, @Param("agora") LocalDateTime agora);
}
