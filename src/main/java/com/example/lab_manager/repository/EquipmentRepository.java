package com.example.lab_manager.repository;

import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.model.Equipment;
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
public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {
    List<Equipment> findByStatus(EquipmentStatus status);
    boolean existsByHeritage(String heritage);
    boolean existsByHeritageAndIdNot(String heritage, UUID id);

    /** RN08: trava o equipamento (SELECT ... FOR UPDATE) para que dois empréstimos simultâneos não passem. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM Equipment e WHERE e.id = :id")
    Optional<Equipment> findByIdForUpdate(@Param("id") UUID id);
}
