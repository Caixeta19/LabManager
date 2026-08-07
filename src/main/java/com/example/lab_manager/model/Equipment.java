package com.example.lab_manager.model;
import com.example.lab_manager.enums.EquipmentStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table( name ="equipments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Equipment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

   @Column(nullable = false)
    private String name;

   @Column(nullable = false)
    private String heritage;

   private String description;


   @Enumerated(EnumType.STRING)
    @Builder.Default
    @Column(nullable = false)

    private EquipmentStatus status = EquipmentStatus.DISPONIVEL;

   @ManyToOne
    @JoinColumn(name = "laboratory_id", nullable = false)
    private Laboratory laboratory;

   @OneToMany(mappedBy = "equipments")
    private List<Loan> loans;



}
