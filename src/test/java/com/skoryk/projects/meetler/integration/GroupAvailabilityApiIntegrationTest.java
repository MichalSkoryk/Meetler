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

class GroupAvailabilityApiIntegrationTest extends AbstractIntegrationTest {

  @Test
  void inviteMemberSelectTemplatesAndBuildAvailabilityGrid() throws Exception {
    String ownerToken = registerAndReturnAccessToken("owner-" + UUID.randomUUID() + "@example.com");
    String memberToken =
        registerAndReturnAccessToken("member-" + UUID.randomUUID() + "@example.com");
    String ownerUserId = getCurrentUserId(ownerToken);
    String memberUserId = getCurrentUserId(memberToken);

    String ownerTemplateId = createTemplate(ownerToken, "Owner availability", "AVAILABLE", true);
    addAvailableBlock(
        ownerToken, ownerTemplateId, "2026-06-08T10:00:00+02:00", "2026-06-08T10:30:00+02:00");

    String memberTemplateId = createTemplate(memberToken, "Member busy by default", "BUSY", true);

    String groupId = createGroup(ownerToken);
    String inviteCode = createInvite(ownerToken, groupId);
    joinGroup(memberToken, inviteCode);

    selectTemplate(ownerToken, groupId, ownerTemplateId);
    selectTemplate(memberToken, groupId, memberTemplateId);

    MvcResult gridResult =
        mockMvc
            .perform(
                get("/api/groups/{groupId}/availability-grid", groupId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                    .queryParam("from", "2026-06-08T10:00:00+02:00")
                    .queryParam("to", "2026-06-08T10:30:00+02:00"))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode firstSlot = body(gridResult).get(0);
    assertThat(firstSlot.get("availableCount").asInt()).isEqualTo(1);
    assertThat(firstSlot.get("busyCount").asInt()).isEqualTo(1);
    assertThat(firstSlot.get("noTemplateCount").asInt()).isZero();

    MvcResult filteredGridResult =
        mockMvc
            .perform(
                get("/api/groups/{groupId}/availability-grid", groupId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(ownerToken))
                    .queryParam("from", "2026-06-08T10:00:00+02:00")
                    .queryParam("to", "2026-06-08T10:30:00+02:00")
                    .queryParam("memberIds", memberUserId))
            .andExpect(status().isOk())
            .andReturn();

    JsonNode filteredFirstSlot = body(filteredGridResult).get(0);
    assertThat(filteredFirstSlot.get("totalMemberCount").asInt()).isEqualTo(1);
    assertThat(filteredFirstSlot.get("availableCount").asInt()).isZero();
    assertThat(filteredFirstSlot.get("busyCount").asInt()).isEqualTo(1);
    assertThat(filteredFirstSlot.get("busyUserIds").get(0).asText()).isEqualTo(memberUserId);
    assertThat(filteredFirstSlot.get("availableUserIds").size()).isZero();
    assertThat(filteredFirstSlot.get("busyUserIds").toString()).doesNotContain(ownerUserId);
  }

  private String registerAndReturnAccessToken(String email) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/auth/register")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(registerRequest(email))))
            .andExpect(status().isOk())
            .andReturn();
    return body(result).get("accessToken").asText();
  }

  private String createTemplate(
      String token, String name, String defaultAvailabilityStatus, boolean isDefault)
      throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/availability/templates")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        json(
                            Map.of(
                                "name", name,
                                "timezone", "Europe/Warsaw",
                                "isDefault", isDefault,
                                "defaultAvailabilityStatus", defaultAvailabilityStatus))))
            .andExpect(status().isOk())
            .andReturn();
    return body(result).get("id").asText();
  }

  private String getCurrentUserId(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(get("/api/me/bootstrap").header(HttpHeaders.AUTHORIZATION, bearer(token)))
            .andExpect(status().isOk())
            .andReturn();
    return body(result).get("user").get("id").asText();
  }

  private void addAvailableBlock(String token, String templateId, String startsAt, String endsAt)
      throws Exception {
    mockMvc
        .perform(
            post("/api/availability/templates/{templateId}/blocks", templateId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    json(
                        Map.of(
                            "startsAt",
                            startsAt,
                            "endsAt",
                            endsAt,
                            "status",
                            "AVAILABLE",
                            "note",
                            "Free"))))
        .andExpect(status().isOk());
  }

  private String createGroup(String token) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/groups")
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("name", "Weekend plans"))))
            .andExpect(status().isOk())
            .andReturn();
    return body(result).get("id").asText();
  }

  private String createInvite(String token, String groupId) throws Exception {
    MvcResult result =
        mockMvc
            .perform(
                post("/api/invites/{groupId}", groupId)
                    .header(HttpHeaders.AUTHORIZATION, bearer(token))
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json(Map.of("maxUses", 10))))
            .andExpect(status().isOk())
            .andReturn();
    return body(result).get("code").asText();
  }

  private void joinGroup(String token, String inviteCode) throws Exception {
    mockMvc
        .perform(
            post("/api/invites/join/{code}", inviteCode)
                .header(HttpHeaders.AUTHORIZATION, bearer(token)))
        .andExpect(status().isOk());
  }

  private void selectTemplate(String token, String groupId, String templateId) throws Exception {
    mockMvc
        .perform(
            post("/api/groups/{groupId}/members/me/availability-template", groupId)
                .header(HttpHeaders.AUTHORIZATION, bearer(token))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(Map.of("availabilityTemplateId", templateId))))
        .andExpect(status().isOk());
  }
}
