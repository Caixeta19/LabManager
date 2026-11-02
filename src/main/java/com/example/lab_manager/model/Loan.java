package com.example.lab_manager.model;

import com.example.lab_manager.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "loans")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Loan {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "equipment_id", nullable = false)
    private Equipment equipment;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime dataRetirada = LocalDateTime.now();

    /** A devolução vale até o fim deste dia (decisão 14). */
    @Column(nullable = false)
    private LocalDate dataDevolucaoPrevista;

    private LocalDateTime dataDevolucaoReal;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private LoanStatus status = LoanStatus.ATIVO;
}
