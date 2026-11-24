package com.example.lab_manager.model;

import com.example.lab_manager.enums.LaboratoryStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "laboratories")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Laboratory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String localization;

    @Column(nullable = false)
    private Integer capacity;

    private String description;

    @Builder.Default
    @Column(nullable = false)
    private Boolean active = true;

    /** Em manutenção o laboratório continua ativo, mas não aceita novas reservas. */
    @Builder.Default
    @Column(nullable = false)
    private boolean maintenance = false;

    @Column(name = "opening_hours", length = 120)
    private String openingHours;

    /** Janela diária para ALUNO/PROFESSOR reservarem. As duas nulas = sem restrição. ADMIN ignora. */
    @Column(name = "booking_open_time")
    private LocalTime bookingOpenTime;

    @Column(name = "booking_close_time")
    private LocalTime bookingCloseTime;

    @Convert(converter = StringListConverter.class)
    @Column(columnDefinition = "TEXT")
    private List<String> resources;

    @OneToMany(mappedBy = "laboratory")
    private List<Equipment> equipments;

    @OneToMany(mappedBy = "laboratory")
    private List<Reserve> reserves;

    public boolean hasBookingWindow() {
        return bookingOpenTime != null && bookingCloseTime != null;
    }

    public LaboratoryStatus status() {
        if (!Boolean.TRUE.equals(active)) {
            return LaboratoryStatus.INATIVO;
        }
        return maintenance ? LaboratoryStatus.MANUTENCAO : LaboratoryStatus.ATIVO;
    }

    public void applyStatus(LaboratoryStatus status) {
        active = status != LaboratoryStatus.INATIVO;
        maintenance = status == LaboratoryStatus.MANUTENCAO;
    }
}
