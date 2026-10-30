package com.example.lab_manager.model;
import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table( name = "laboratories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Laboratory {

    @Id
    @GeneratedValue (strategy = GenerationType.UUID )
    private UUID id;

   @Column(nullable = false)
    private String name;

   @Column(nullable = false)
    private String locatization;

   @Column(nullable = false)
    private Integer capacity;

   private String description;

   @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

   @OneToMany(mappedBy = "laboratory")
    private List<Equipment> equipments;

   @OneToMany(mappedBy = "laboratory")
    private List<Reserve> reserves;







}
