package com.skoryk.projects.meetler.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
  @Email @NotBlank private String email;

  @Size(max = 120)
  private String name;

  @NotBlank
  @Size(min = 8, max = 128)
  private String password;
}
