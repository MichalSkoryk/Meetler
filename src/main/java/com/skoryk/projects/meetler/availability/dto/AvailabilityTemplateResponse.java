package com.skoryk.projects.meetler.availability.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AvailabilityTemplateResponse {

  private UUID id;
  private String name;
  @JsonProperty("isDefault")
  private boolean isDefault;
  private String timezone;
  private AvailabilityBlockStatus defaultAvailabilityStatus;
  private OffsetDateTime createdAt;
  private OffsetDateTime updatedAt;
}
