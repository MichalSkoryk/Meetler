package com.skoryk.projects.meetler.group.event.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupEventParticipantResponse {

  private UUID userId;
  private String userEmail;
  private String status;
  private OffsetDateTime respondedAt;
}
