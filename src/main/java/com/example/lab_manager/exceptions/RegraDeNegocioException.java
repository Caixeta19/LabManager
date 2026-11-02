package com.example.lab_manager.exceptions;

import org.springframework.http.HttpStatus;

/**
 * Violação de regra de negócio. O padrão é 400; algumas regras usam 409 (decisões 4 e 8, duplicidades).
 */
public class RegraDeNegocioException extends LabManagerException {
  public RegraDeNegocioException(String message) {
    super(message, "ERR_BUSINESS_001", HttpStatus.BAD_REQUEST);
  }

  public RegraDeNegocioException(String message, String codigoInterno) {
    super(message, codigoInterno, HttpStatus.BAD_REQUEST);
  }

  public RegraDeNegocioException(String message, String codigoInterno, HttpStatus httpStatus) {
    super(message, codigoInterno, httpStatus);
  }
}
