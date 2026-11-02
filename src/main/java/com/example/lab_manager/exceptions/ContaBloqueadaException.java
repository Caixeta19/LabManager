package com.example.lab_manager.exceptions;

import org.springframework.http.HttpStatus;

/** Conta bloqueada por excesso de tentativas de login (decisão 3) → 423. */
public class ContaBloqueadaException extends LabManagerException {
    public ContaBloqueadaException() {
        super("Conta bloqueada temporariamente por excesso de tentativas. Tente novamente mais tarde.",
                "ERR_AUTH_LOCKED", HttpStatus.LOCKED);
    }
}
