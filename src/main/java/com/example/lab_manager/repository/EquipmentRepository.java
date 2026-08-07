package com.example.lab_manager.repository;
import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.model.Equipment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EquipmentRepository extends JpaRepository<Equipment, UUID> {

    List<Equipment> findByLaboratory_Id(UUID laboratoryId);

    List<Equipment> findByStatus(EquipmentStatus status);






}
