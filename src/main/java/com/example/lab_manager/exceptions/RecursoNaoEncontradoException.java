package com.example.lab_manager.exceptions;

import org.springframework.http.HttpStatus;

public class RecursoNaoEncontradoException extends LabManagerException {
    public RecursoNaoEncontradoException(String message) {
        super(message, "ERR_NOT_FOUND", HttpStatus.NOT_FOUND);
    }
}