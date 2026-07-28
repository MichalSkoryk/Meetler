package com.skoryk.projects.meetler.group.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupResponse {
  private UUID id;
  private String name;
  private String role;
  private long memberCount;
  private boolean hasAvailabilityTemplate;
  private long pendingResponseCount;
  private GroupEventSummaryResponse nextEvent;
  private GroupEventSummaryResponse nextPendingEvent;
  private boolean eventRequiresConfirmation;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
