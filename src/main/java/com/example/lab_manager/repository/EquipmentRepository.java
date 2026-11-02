package com.example.lab_manager.repository;

import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {
    List<Equipment> findByStatus(EquipmentStatus status);
    boolean existsByHeritage(String heritage);
    boolean existsByHeritageAndIdNot(String heritage, UUID id);
}
