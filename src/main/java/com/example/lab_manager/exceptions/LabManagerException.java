package com.example.lab_manager.exceptions;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public abstract class LabManagerException extends RuntimeException {
    private final String codigoInterno;
    private final HttpStatus httpStatus;

    public LabManagerException(String message, String codigoInterno, HttpStatus httpStatus) {
        super(message);
        this.codigoInterno = codigoInterno;
        this.httpStatus = httpStatus;
    }
}