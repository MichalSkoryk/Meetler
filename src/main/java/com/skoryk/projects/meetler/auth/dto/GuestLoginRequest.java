package com.skoryk.projects.meetler.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GuestLoginRequest {

  @Email @NotBlank private String email;

  private String name;
}
