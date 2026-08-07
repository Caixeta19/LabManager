package com.example.lab_manager.repository;

import com.example.lab_manager.enums.LoanStatus;
import com.example.lab_manager.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {

    List<Loan> findByUserId (UUID userId);

    List<Loan> findByStatus (LoanStatus status);

    List<Loan> findByEquipmentId (UUID equipmentId);



}
