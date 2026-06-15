package com.skoryk.projects.meetler.common;

import com.skoryk.projects.meetler.subscription.SubscriptionLimitExceededException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.context.request.async.AsyncRequestTimeoutException;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<Map<String, Object>> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fields.put(error.getField(), error.getDefaultMessage());
    }

    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", fields, request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<Map<String, Object>> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    Map<String, String> fields = new LinkedHashMap<>();
    ex.getConstraintViolations()
        .forEach(
            violation ->
                fields.put(violation.getPropertyPath().toString(), violation.getMessage()));

    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", fields, request);
  }

  @ExceptionHandler(HandlerMethodValidationException.class)
  public ResponseEntity<Map<String, Object>> handleHandlerMethodValidation(
      HandlerMethodValidationException ex, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Validation failed", null, request);
  }

  @ExceptionHandler({
    MissingServletRequestParameterException.class,
    MethodArgumentTypeMismatchException.class,
    IllegalArgumentException.class
  })
  public ResponseEntity<Map<String, Object>> handleBadRequest(
      Exception ex, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, "BAD_REQUEST", ex.getMessage(), null, request);
  }

  @ExceptionHandler(SubscriptionLimitExceededException.class)
  public ResponseEntity<Map<String, Object>> handleSubscriptionLimit(
      SubscriptionLimitExceededException ex, HttpServletRequest request) {
    return error(
        HttpStatus.CONFLICT, "SUBSCRIPTION_LIMIT_EXCEEDED", ex.getMessage(), null, request);
  }

  @ExceptionHandler(IllegalStateException.class)
  public ResponseEntity<Map<String, Object>> handleConflict(
      IllegalStateException ex, HttpServletRequest request) {
    return error(HttpStatus.CONFLICT, "CONFLICT", ex.getMessage(), null, request);
  }

  @ExceptionHandler(AccessDeniedException.class)
  public ResponseEntity<Map<String, Object>> handleAccessDenied(
      AccessDeniedException ex, HttpServletRequest request) {
    return error(HttpStatus.FORBIDDEN, "FORBIDDEN", ex.getMessage(), null, request);
  }

  @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
  public ResponseEntity<Map<String, Object>> handleMethodNotSupported(
      HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
    return error(
        HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", ex.getMessage(), null, request);
  }

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<Map<String, Object>> handleResponseStatus(
      ResponseStatusException ex, HttpServletRequest request) {
    HttpStatusCode statusCode = ex.getStatusCode();
    HttpStatus status = HttpStatus.resolve(statusCode.value());
    String reason = ex.getReason() == null ? statusCode.toString() : ex.getReason();
    return error(
        status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status,
        statusCode.value() == 404 ? "NOT_FOUND" : "REQUEST_FAILED",
        reason,
        null,
        request);
  }

  @ExceptionHandler(AsyncRequestTimeoutException.class)
  public ResponseEntity<Map<String, Object>> handleTimeout(
      AsyncRequestTimeoutException ex, HttpServletRequest request) {
    return error(
        HttpStatus.SERVICE_UNAVAILABLE, "REQUEST_TIMEOUT", "Request timed out", null, request);
  }

  @ExceptionHandler(RestClientResponseException.class)
  public ResponseEntity<Map<String, Object>> handleExternalServiceResponse(
      RestClientResponseException ex, HttpServletRequest request) {
    log.warn(
        "External service request failed while handling {} {} with status {}",
        request.getMethod(),
        request.getRequestURI(),
        ex.getStatusCode().value(),
        ex);
    String responseBody = truncate(ex.getResponseBodyAsString(), 500);
    String message = "External service request failed with status " + ex.getStatusCode().value();
    if (responseBody != null && !responseBody.isBlank()) {
      message += ": " + responseBody;
    }
    return error(HttpStatus.BAD_GATEWAY, "EXTERNAL_SERVICE_ERROR", message, null, request);
  }

  @ExceptionHandler(RestClientException.class)
  public ResponseEntity<Map<String, Object>> handleExternalService(
      RestClientException ex, HttpServletRequest request) {
    log.warn(
        "External service request failed while handling {} {}",
        request.getMethod(),
        request.getRequestURI(),
        ex);
    return error(
        HttpStatus.BAD_GATEWAY,
        "EXTERNAL_SERVICE_ERROR",
        "External service request failed",
        null,
        request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<Map<String, Object>> handleUnexpected(
      Exception ex, HttpServletRequest request) {
    log.error(
        "Unexpected error while handling {} {}", request.getMethod(), request.getRequestURI(), ex);
    return error(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_ERROR",
        "Unexpected server error",
        null,
        request);
  }

  private ResponseEntity<Map<String, Object>> error(
      HttpStatus status,
      String code,
      String message,
      Map<String, String> fields,
      HttpServletRequest request) {
    Map<String, Object> body = new LinkedHashMap<>();
    body.put("timestamp", OffsetDateTime.now());
    body.put("status", status.value());
    body.put("error", status.getReasonPhrase());
    body.put("code", code);
    body.put("message", message);
    body.put("path", request.getRequestURI());

    if (fields != null && !fields.isEmpty()) {
      body.put("fields", fields);
    }

    return ResponseEntity.status(status).body(body);
  }

  private String truncate(String message, int maxLength) {
    if (message == null || message.length() <= maxLength) {
      return message;
    }
    return message.substring(0, maxLength);
  }
}
