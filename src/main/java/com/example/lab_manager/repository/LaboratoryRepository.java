package com.example.lab_manager.repository;

import com.example.lab_manager.model.Laboratory;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LaboratoryRepository extends JpaRepository<Laboratory, UUID> {
    List<Laboratory> findByActiveTrue();

    /**
     * Trava a linha do laboratório (SELECT ... FOR UPDATE) até o fim da transação.
     * Serializa a criação de reservas no mesmo laboratório, evitando duas reservas no mesmo horário.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT l FROM Laboratory l WHERE l.id = :id")
    Optional<Laboratory> findByIdForUpdate(@Param("id") UUID id);
}
