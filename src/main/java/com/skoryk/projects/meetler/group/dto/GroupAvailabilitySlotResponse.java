package com.skoryk.projects.meetler.group.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class GroupAvailabilitySlotResponse {

  private OffsetDateTime startsAt;
  private OffsetDateTime endsAt;
  private int totalMemberCount;
  private int availableCount;
  private int busyCount;
  private int noTemplateCount;
  private List<UUID> availableUserIds;
  private List<UUID> busyUserIds;
  private List<UUID> noTemplateUserIds;
}
