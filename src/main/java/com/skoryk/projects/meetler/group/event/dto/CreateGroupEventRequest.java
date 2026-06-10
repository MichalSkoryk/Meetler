package com.skoryk.projects.meetler.group.event.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import lombok.Data;

@Data
public class CreateGroupEventRequest {

  @NotBlank
  @Size(max = 255)
  private String title;

  @Size(max = 2000)
  private String description;

  @NotNull private OffsetDateTime startsAt;

  @NotNull private OffsetDateTime endsAt;
}
