package com.tabloncitos.backend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<String> handleRuntimeException(RuntimeException ex) {
        String jsonError = "{\"error\": \"No encontrado / Error de Negocio\", \"message\": \"" + ex.getMessage() + "\"}";
        return new ResponseEntity<>(jsonError, HttpStatus.NOT_FOUND);
    }
}