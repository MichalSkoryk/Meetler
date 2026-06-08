package com.skoryk.projects.meetler.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

class AuthCalendarApiIntegrationTest extends AbstractIntegrationTest {

  @Test
  void registerThenUseTokenToCreateAndListCalendar() throws Exception {
    String email = "calendar-" + UUID.randomUUID() + "@example.com";
    MvcResult registerResult =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(registerRequest(email))))
            .andExpect(status().isOk())
            .andReturn();

    String accessToken = body(registerResult).get("accessToken").asText();

    mockMvc.perform(get("/api/calendars")).andExpect(status().isForbidden());

    MvcResult createCalendarResult =
        mockMvc
            .perform(
                post("/api/calendars")
                    .header(HttpHeaders.AUTHORIZATION, bearer(accessToken))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json(
                            Map.of(
                                "name", "Private",
                                "provider", "INTERNAL",
                                "color", "#4f46e5",
                                "isEditable", true,
                                "syncDirection", "NONE"))))
            .andExpect(status().isOk())
            .andReturn();

    assertThat(body(createCalendarResult).get("name").asText()).isEqualTo("Private");

    MvcResult listCalendarResult =
        mockMvc
            .perform(get("/api/calendars").header(HttpHeaders.AUTHORIZATION, bearer(accessToken)))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode calendars = body(listCalendarResult);
    assertThat(calendars).hasSize(1);
    assertThat(calendars.get(0).get("provider").asText()).isEqualTo("INTERNAL");
  }
}
