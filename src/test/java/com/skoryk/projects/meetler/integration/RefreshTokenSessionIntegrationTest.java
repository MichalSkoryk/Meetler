package com.skoryk.projects.meetler.integration;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class RefreshTokenSessionIntegrationTest extends AbstractIntegrationTest {

  @Test
  void sessionsRotateAndLogoutIndependently() throws Exception {
    String email = "sessions-" + UUID.randomUUID() + "@example.com";
    MvcResult registration =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(registerRequest(email))))
            .andExpect(status().isOk())
            .andReturn();
    String firstSession = body(registration).get("refreshToken").asText();

    MvcResult login =
        mockMvc
            .perform(
                post("/api/auth/login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("email", email, "password", "password123"))))
            .andExpect(status().isOk())
            .andReturn();
    String secondSession = body(login).get("refreshToken").asText();

    String rotatedFirstSession = refresh(firstSession);
    String rotatedSecondSession = refresh(secondSession);

    mockMvc
        .perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refreshToken", firstSession))))
        .andExpect(status().isBadRequest());

    mockMvc
        .perform(
            post("/api/auth/logout")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("refreshToken", rotatedFirstSession))))
        .andExpect(status().isNoContent());

    refresh(rotatedSecondSession);
  }

  private String refresh(String refreshToken) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/refresh")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("refreshToken", refreshToken))))
            .andExpect(status().isOk())
            .andReturn();
    return body(result).get("refreshToken").asText();
  }
}
