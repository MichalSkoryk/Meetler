package com.skoryk.projects.meetler.group.event.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupEventResponse {

  private UUID id;
  private UUID groupId;
  private UUID createdByUserId;
  private String title;
  private String description;
  private OffsetDateTime startsAt;
  private OffsetDateTime endsAt;
  private String status;
  private boolean requiresConfirmation;
  private List<GroupEventParticipantResponse> participants;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
