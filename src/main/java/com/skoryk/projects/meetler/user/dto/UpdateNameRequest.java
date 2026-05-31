package com.skoryk.projects.meetler.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateNameRequest {
  @NotBlank
  @Size(max = 120)
  private String name;
}
