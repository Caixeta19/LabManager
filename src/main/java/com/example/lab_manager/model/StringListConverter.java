package com.example.lab_manager.model;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

import java.util.Arrays;
import java.util.List;

/** Guarda uma lista de textos numa coluna TEXT, um item por linha. */
@Converter
public class StringListConverter implements AttributeConverter<List<String>, String> {

    private static final String SEPARADOR = "\n";

    @Override
    public String convertToDatabaseColumn(List<String> itens) {
        return itens == null || itens.isEmpty() ? null : String.join(SEPARADOR, itens);
    }

    @Override
    public List<String> convertToEntityAttribute(String coluna) {
        return coluna == null || coluna.isBlank() ? List.of() : Arrays.asList(coluna.split(SEPARADOR));
    }
}
