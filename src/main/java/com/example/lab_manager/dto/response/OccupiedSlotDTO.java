package com.example.lab_manager.dto.response;

import java.time.LocalDateTime;

/** Horário ocupado de um laboratório. Não diz quem reservou. */
public record OccupiedSlotDTO(LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim) {}
