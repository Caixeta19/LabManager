package com.example.lab_manager.repository;

import com.example.lab_manager.model.Laboratory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LaboratoryRepository extends JpaRepository<Laboratory, UUID> {
    List<Laboratory> findByActiveTrue();
}