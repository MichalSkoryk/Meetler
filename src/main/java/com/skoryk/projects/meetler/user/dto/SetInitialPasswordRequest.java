package com.skoryk.projects.meetler.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetInitialPasswordRequest {
  @NotBlank
  @Size(min = 8, max = 128)
  private String initialPassword;
}
