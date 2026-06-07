package com.skoryk.projects.meetler.availability.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateAvailabilityTemplateRequest {

  @NotBlank
  @Size(max = 255)
  private String name;

  @NotNull
  private boolean isDefault;

  @NotBlank
  @Size(max = 64)
  private String timezone;
}

