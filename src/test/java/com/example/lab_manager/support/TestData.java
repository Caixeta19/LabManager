package com.example.lab_manager.support;

import com.example.lab_manager.enums.EquipmentStatus;
import com.example.lab_manager.enums.LoanStatus;
import com.example.lab_manager.enums.ReserveStatus;
import com.example.lab_manager.enums.UserType;
import com.example.lab_manager.model.Equipment;
import com.example.lab_manager.model.Laboratory;
import com.example.lab_manager.model.Loan;
import com.example.lab_manager.model.Reserve;
import com.example.lab_manager.model.User;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

/** Fixtures dos testes unitários. Nenhum teste unitário acessa banco. */
public final class TestData {

    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    /** "Agora" fixo de todos os testes: 02/11/2026 10:00. */
    public static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 2, 10, 0);
    public static final Clock CLOCK = Clock.fixed(AGORA.atZone(ZONE).toInstant(), ZONE);

    private TestData() {}

    public static User user(UserType type) {
        return User.builder()
                .id(UUID.randomUUID())
                .name("Usuário " + type)
                .email(type.name().toLowerCase() + "-" + UUID.randomUUID() + "@lab.com")
                .password("$2a$10$hash")
                .registration("MAT-" + UUID.randomUUID())
                .type(type)
                .build();
    }

    public static Laboratory lab(boolean active) {
        return Laboratory.builder()
                .id(UUID.randomUUID())
                .name("Lab de Redes")
                .localization("Bloco B")
                .capacity(30)
                .active(active)
                .build();
    }

    public static Reserve reserve(User user, Laboratory lab, ReserveStatus status,
                                  LocalDateTime inicio, LocalDateTime fim) {
        return Reserve.builder()
                .id(UUID.randomUUID())
                .user(user)
                .laboratory(lab)
                .status(status)
                .dataHoraInicio(inicio)
                .dataHoraFim(fim)
                .build();
    }

    public static Equipment equipment(EquipmentStatus status) {
        return Equipment.builder()
                .id(UUID.randomUUID())
                .name("Osciloscópio")
                .heritage("PAT-" + UUID.randomUUID())
                .status(status)
                .laboratory(lab(true))
                .build();
    }

    public static Loan loan(User user, Equipment equipment, LoanStatus status) {
        return Loan.builder()
                .id(UUID.randomUUID())
                .user(user)
                .equipment(equipment)
                .dataRetirada(AGORA.minusDays(3))
                .dataDevolucaoPrevista(LocalDate.from(AGORA).minusDays(1))
                .status(status)
                .build();
    }
}
