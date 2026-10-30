package com.example.lab_manager.exceptions;

import org.springframework.http.HttpStatus;

public class HorarioConflitanteException extends LabManagerException {
    public HorarioConflitanteException(String message) {
        super(message, "ERR_RESERVA_001", HttpStatus.CONFLICT);
    }
}