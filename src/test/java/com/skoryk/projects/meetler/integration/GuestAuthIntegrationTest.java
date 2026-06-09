package com.skoryk.projects.meetler.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.net.URI;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class GuestAuthIntegrationTest extends AbstractIntegrationTest {

  @Test
  void guestCanLoginWithMagicLinkAndCreateOnlyOneTemplate() throws Exception {
    MvcResult requestResult =
        mockMvc
            .perform(
                post("/api/auth/guest/request-login")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json(
                            Map.of(
                                "email", "guest-it@example.com",
                                "name", "Guest Integration"))))
            .andExpect(status().isOk())
            .andReturn();

    String loginLink = body(requestResult).get("loginLink").asText();
    String token = URI.create(loginLink).getRawQuery().replace("token=", "");

    MvcResult loginResult =
        mockMvc
            .perform(get("/api/auth/guest/login").param("token", token))
            .andExpect(status().isOk())
            .andReturn();

    String accessToken = body(loginResult).get("accessToken").asText();

    mockMvc
        .perform(
            post("/api/availability/templates")
                .header("Authorization", bearer(accessToken))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(templateRequest("Guest template"))))
        .andExpect(status().isOk());

    MvcResult secondTemplateResult =
        mockMvc
            .perform(
                post("/api/availability/templates")
                    .header("Authorization", bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(templateRequest("Second guest template"))))
            .andExpect(status().isConflict())
            .andReturn();

    JsonNode errorBody = body(secondTemplateResult);
    assertThat(errorBody.get("message").asText())
        .isEqualTo("Guest accounts can create only one availability template");
  }

  private Map<String, Object> templateRequest(String name) {
    return Map.of(
        "name",
        name,
        "timezone",
        "Europe/Warsaw",
        "default",
        true,
        "defaultAvailabilityStatus",
        "BUSY");
  }
}
