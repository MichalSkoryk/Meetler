package com.skoryk.projects.meetler.common;

import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(MethodArgumentNotValidException ex) {
    Map<String, String> fields = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fields.put(error.getField(), error.getDefaultMessage());
    }

    return error(HttpStatus.BAD_REQUEST, "Validation failed", fields);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, Object>> handleConstraintViolation(
      ConstraintViolationException ex) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getConstraintViolations()
        .forEach(
            violation ->
                fields.put(violation.getPropertyPath().toString(), violation.getMessage()));

    return error(HttpStatus.BAD_REQUEST, "Validation failed", fields);
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(IllegalStateException ex) {
    return error(HttpStatus.CONFLICT, ex.getMessage(), null);
  }

  private ResponseEntity<Map<String, Object>> error(
      HttpStatus status, String message, Map<String, String> fields) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", OffsetDateTime.now());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("message", message);

    if (fields != null && !fields.isEmpty()) {
      body.put("fields", fields);
    }

    return ResponseEntity.status(status).body(body);
  }
}
