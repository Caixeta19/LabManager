package com.example.lab_manager.exceptions;

import org.springframework.http.HttpStatus;

public class ReservaNaoConfirmadaException extends LabManagerException {
    public ReservaNaoConfirmadaException(String message) {
        super(message, "ERR_CHECKIN_001", HttpStatus.UNPROCESSABLE_ENTITY);
    }
}
