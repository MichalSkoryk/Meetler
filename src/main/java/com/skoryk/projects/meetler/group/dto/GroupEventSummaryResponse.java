package com.skoryk.projects.meetler.group.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupEventSummaryResponse {
  private UUID id;
  private String title;
  private OffsetDateTime startsAt;
  private OffsetDateTime endsAt;
  private String status;
  private boolean requiresConfirmation;
  private String myResponseStatus;
}
