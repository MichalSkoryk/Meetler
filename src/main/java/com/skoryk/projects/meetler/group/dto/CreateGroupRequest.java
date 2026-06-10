package com.skoryk.projects.meetler.group.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateGroupRequest {
  @NotBlank
  @Size(max = 255)
  private String name;

  private Boolean eventRequiresConfirmation;
}
