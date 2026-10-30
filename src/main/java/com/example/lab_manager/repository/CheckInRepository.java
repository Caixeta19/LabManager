package com.example.lab_manager.repository;

import com.example.lab_manager.model.CheckIn;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface CheckInRepository extends JpaRepository<CheckIn, UUID> {
    boolean existsByReserveId(UUID reserveId);
}