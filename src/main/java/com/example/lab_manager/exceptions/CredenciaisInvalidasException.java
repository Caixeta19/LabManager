package com.example.lab_manager.exceptions;

import com.example.lab_manager.exceptions.LabManagerException;
import org.springframework.http.HttpStatus;

public class CredenciaisInvalidasException extends LabManagerException {
    public CredenciaisInvalidasException(String message) {
        super(message, "ERR_AUTH_001", HttpStatus.UNAUTHORIZED);
    }
}