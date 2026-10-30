package com.example.lab_manager.repository;

import com.example.lab_manager.model.Loan;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

public interface LoanRepository extends JpaRepository<Loan, UUID> {
}