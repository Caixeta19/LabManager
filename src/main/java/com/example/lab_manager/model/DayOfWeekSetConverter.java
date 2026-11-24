package com.example.lab_manager.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.time.DayOfWeek;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.stream.Collectors;

/** Dias da semana numa coluna de texto, como números ISO (1 = segunda … 7 = domingo): "1,2,3,4,5". */
@Converter
public class DayOfWeekSetConverter implements AttributeConverter<Set<DayOfWeek>, String> {

    @Override
    public String convertToDatabaseColumn(Set<DayOfWeek> dias) {
        if (dias == null || dias.isEmpty()) {
            return null;
        }
        return dias.stream().sorted().map(d -> String.valueOf(d.getValue())).collect(Collectors.joining(","));
    }

    @Override
    public Set<DayOfWeek> convertToEntityAttribute(String coluna) {
        if (coluna == null || coluna.isBlank()) {
            return EnumSet.noneOf(DayOfWeek.class);
        }
        return Arrays.stream(coluna.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> DayOfWeek.of(Integer.parseInt(s)))
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(DayOfWeek.class)));
    }
}
