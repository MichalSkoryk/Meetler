package com.skoryk.projects.meetler.availability.dto;

import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailabilityTemplateResponse {

  private UUID id;
  private String name;
  private boolean isDefault;
  private String timezone;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}

