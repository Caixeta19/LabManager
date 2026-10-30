package com.example.lab_manager.exceptions;

import org.springframework.http.HttpStatus;

public class RegraDeNegocioException extends LabManagerException {
  public RegraDeNegocioException(String message) {
    super(message, "ERR_BUSINESS_001", HttpStatus.BAD_REQUEST);
  }

  public RegraDeNegocioException(String message, String codigoInterno) {
    super(message, codigoInterno, HttpStatus.BAD_REQUEST);
  }
}