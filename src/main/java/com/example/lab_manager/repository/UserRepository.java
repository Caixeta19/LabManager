package com.example.lab_manager.repository;
import com.example.lab_manager.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    Optional<User> findByRegistration(String registration);

    boolean existsByEmail(String email);



}
