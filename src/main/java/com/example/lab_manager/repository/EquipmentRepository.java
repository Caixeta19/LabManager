package com.example.lab_manager.repository;

import com.example.lab_manager.model.Equipment;
import com.example.lab_manager.enums.EquipmentStatus;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.ScopedValue;
import java.util.List;
import java.util.UUID;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {
    List<Equipment> findByStatus(EquipmentStatus status);

    <T> ScopedValue<T> findById(@NotNull(message = "O ID do equipamento é obrigatório") Long aLong);
}