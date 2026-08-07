package com.example.lab_manager.model;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "checkins")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder

public class CheckIn {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(name = "reserve_id", nullable = false, unique = true)
    private Reserve reserve;

    @Builder.Default
    @Column(nullable = false)
    private LocalDateTime horarioChegada = LocalDateTime.now();



}
