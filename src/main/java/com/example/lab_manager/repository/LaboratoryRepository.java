package com.example.lab_manager.repository;

import com.example.lab_manager.model.Laboratory;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.lang.ScopedValue;
import java.util.List;
import java.util.UUID;

@Repository
public interface LaboratoryRepository extends JpaRepository<Laboratory, UUID> {
    List<Laboratory> findByActiveTrue();

    <T> ScopedValue<T> findById(@NotNull(message = "O ID do laboratório é obrigatório") Long aLong);
}