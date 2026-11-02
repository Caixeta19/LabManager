package com.example.lab_manager.enums;

/**
 * Máquina de estados da reserva (RN13 + decisão 1).
 * <pre>
 * PENDENTE   → CONFIRMADA  (link do e-mail)
 * PENDENTE   → CANCELADA   (cancelamento sem confirmação)
 * PENDENTE   → EXPIRADA    (job: início passou sem confirmação)
 * CONFIRMADA → CANCELADA   (cancelamento manual)
 * CANCELADA e EXPIRADA são estados finais.
 * </pre>
 */
public enum ReserveStatus {
    PENDENTE,
    CONFIRMADA,
    CANCELADA,
    EXPIRADA;

    public boolean podeTransicionarPara(ReserveStatus destino) {
        if (destino == null) {
            return false;
        }
        return switch (this) {
            case PENDENTE -> destino == CONFIRMADA || destino == CANCELADA || destino == EXPIRADA;
            case CONFIRMADA -> destino == CANCELADA;
            case CANCELADA, EXPIRADA -> false;
        };
    }
}
