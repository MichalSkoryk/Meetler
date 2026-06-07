package com.skoryk.projects.meetler.availability.dto;

import com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class UpdateAvailabilityBlockRequest {

  @NotNull private OffsetDateTime startsAt;

  @NotNull private OffsetDateTime endsAt;

  @NotNull private AvailabilityBlockStatus status;

  @Size(max = 255)
  private String note;
}

