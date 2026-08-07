package com.example.lab_manager.model;
import com.example.lab_manager.enums.LoanStatus;
import jakarta.persistence.*;
import lombok.*;
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
    @JoinColumn( name = "equipment_id", nullable = false)
    private Equipment equipment;

     @Builder.Default
    @Column(nullable = false)
    private LocalDateTime dataRetirada = LocalDateTime.now();

     @Column(nullable = false)
     private LocalDateTime dataDevolucaoPrevista;

     private LocalDateTime dataDevolucaoReal;

     @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)
    private LoanStatus status = LoanStatus.EM_ANDAMENTO;










}
