package com.skoryk.projects.meetler.availability.dto;

import com.skoryk.projects.meetler.availability.AvailabilityBlockSource;
import com.skoryk.projects.meetler.availability.AvailabilityBlockStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class AddAvailabilityBlockRequest {

  @NotNull private OffsetDateTime startsAt;

  @NotNull private OffsetDateTime endsAt;

  @NotNull private AvailabilityBlockStatus status;

  private AvailabilityBlockSource source = AvailabilityBlockSource.MANUAL;

  @Size(max = 255)
  private String note;
}
