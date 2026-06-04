package com.skoryk.projects.meetler.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateUserRequest {
  @Email @NotBlank private String email;
}
