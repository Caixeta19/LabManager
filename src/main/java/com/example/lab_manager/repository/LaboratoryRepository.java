package com.example.lab_manager.repository;
import com.example.lab_manager.model.Laboratory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LaboratoryRepository extends JpaRepository<Laboratory, UUID> {

    List<Laboratory> findAByActiveTrue();




}
