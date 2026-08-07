package com.example.lab_manager.model;
import jakarta.persistence.*;
import lombok.*;
import java.util.UUID;

@Entity
@Table( name = "laboratory")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class Laboratory {

    @Id
    @GeneratedValue (strategy = GenerationType.UUID )
    private UUID id;








}
