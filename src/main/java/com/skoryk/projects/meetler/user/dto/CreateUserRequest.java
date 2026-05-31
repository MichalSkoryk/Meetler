package com.skoryk.projects.meetler.user.dto;

import com.skoryk.projects.meetler.user.AuthProvider;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {
  @Email @NotBlank private String email;

  @NotNull private AuthProvider authProvider;
}
