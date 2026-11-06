package com.example.lab_manager.model;

import com.example.lab_manager.enums.UserType;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false, unique = true)
    private String registration;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserType type;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    /** Tentativas de login erradas seguidas (decisão 3). */
    @Builder.Default
    @Column(name = "failed_attempts", nullable = false)
    private int failedAttempts = 0;

    /** Enquanto estiver no futuro, o login é recusado com ERR_AUTH_LOCKED. */
    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    /** Último login com sucesso, exibido como "último acesso" no AdminUsers. */
    @Column(name = "last_login_at")
    private LocalDateTime lastLoginAt;

    @OneToMany(mappedBy = "user")
    private List<Reserve> reserves;

    @OneToMany(mappedBy = "user")
    private List<Loan> loans;
}
