package com.skoryk.projects.meetler.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PasswordResetRequest {

  @Email @NotBlank private String email;

  @Size(max = 2048)
  private String returnUrl;
}
