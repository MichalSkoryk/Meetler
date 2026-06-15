package com.skoryk.projects.meetler.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
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
}
