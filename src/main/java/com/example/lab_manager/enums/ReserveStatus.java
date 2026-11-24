package com.example.lab_manager.enums;

/**
 * Máquina de estados da reserva (RN13 + decisão 1).
 * <pre>
 * PENDENTE   → CONFIRMADA  (link do e-mail)
 * PENDENTE   → CANCELADA   (cancelamento sem confirmação)
 * PENDENTE   → EXPIRADA    (job: início passou sem confirmação)
 * CONFIRMADA → CANCELADA   (cancelamento manual)
 * CONFIRMADA → NAO_COMPARECEU (job: a reserva terminou sem check-in)
 * CANCELADA, EXPIRADA e NAO_COMPARECEU são estados finais.
 * </pre>
 */
public enum ReserveStatus {
    PENDENTE,
    CONFIRMADA,
    CANCELADA,
    EXPIRADA,
    NAO_COMPARECEU;

    public boolean podeTransicionarPara(ReserveStatus destino) {
        if (destino == null) {
            return false;
        }
        return switch (this) {
            case PENDENTE -> destino == CONFIRMADA || destino == CANCELADA || destino == EXPIRADA;
            case CONFIRMADA -> destino == CANCELADA || destino == NAO_COMPARECEU;
            case CANCELADA, EXPIRADA, NAO_COMPARECEU -> false;
        };
    }
}
