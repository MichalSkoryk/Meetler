package com.skoryk.projects.meetler.availability.dto;

import com.skoryk.projects.meetler.availability.AvailabilityBlockSource;
import com.skoryk.projects.meetler.availability.AvailabilityBlockStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailabilityBlockResponse {

  private UUID id;
  private OffsetDateTime startsAt;
  private OffsetDateTime endsAt;
  private AvailabilityBlockStatus status;
  private AvailabilityBlockSource source;
  private String note;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
