package com.skoryk.projects.meetler.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.HttpRequestMethodNotSupportedException;

class ApiExceptionHandlerTest {

  private final ApiExceptionHandler handler = new ApiExceptionHandler();

  @Test
  void handleMethodNotSupportedReturns405() {
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/auth/password/reset/confirm");
    HttpRequestMethodNotSupportedException exception =
        new HttpRequestMethodNotSupportedException("GET", List.of("POST"));

    ResponseEntity<Map<String, Object>> response =
        handler.handleMethodNotSupported(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(405);
    assertThat(response.getBody())
        .containsEntry("code", "METHOD_NOT_ALLOWED")
        .containsEntry("path", "/api/auth/password/reset/confirm");
  }

  @Test
  void handleUnreadableJsonReturns400() {
    MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/calendars");
    HttpMessageNotReadableException exception = mock(HttpMessageNotReadableException.class);
    when(exception.getMessage()).thenReturn("Invalid calendar synchronization direction");

    ResponseEntity<Map<String, Object>> response = handler.handleBadRequest(exception, request);

    assertThat(response.getStatusCode().value()).isEqualTo(400);
    assertThat(response.getBody())
        .containsEntry("code", "BAD_REQUEST")
        .containsEntry("path", "/api/calendars");
  }
}
