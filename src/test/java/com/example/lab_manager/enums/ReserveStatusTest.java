package com.example.lab_manager.enums;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;

import static com.example.lab_manager.enums.ReserveStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

/** Máquina de estados (RN13 + decisão 1): cobre as 16 combinações origem × destino. */
class ReserveStatusTest {

    private static final Set<String> PERMITIDAS = Set.of(
            "PENDENTE->CONFIRMADA",
            "PENDENTE->CANCELADA",
            "PENDENTE->EXPIRADA",
            "CONFIRMADA->CANCELADA"
    );

    static Stream<Arguments> todasAsCombinacoes() {
        return Stream.of(values()).flatMap(origem ->
                Stream.of(values()).map(destino -> Arguments.of(origem, destino)));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("todasAsCombinacoes")
    void transicaoSoEPermitidaSeEstiverNaTabela(ReserveStatus origem, ReserveStatus destino) {
        boolean esperado = PERMITIDAS.contains(origem + "->" + destino);
        assertThat(origem.podeTransicionarPara(destino)).isEqualTo(esperado);
    }

    @Test
    void estadosFinaisNaoTransicionamParaNada() {
        for (ReserveStatus destino : values()) {
            assertThat(CANCELADA.podeTransicionarPara(destino)).isFalse();
            assertThat(EXPIRADA.podeTransicionarPara(destino)).isFalse();
        }
    }

    @Test
    void destinoNuloNaoEhPermitido() {
        assertThat(PENDENTE.podeTransicionarPara(null)).isFalse();
    }
}
