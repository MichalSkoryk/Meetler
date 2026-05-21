package com.skoryk.projects.meetler.auth.dto;

import lombok.Data;

@Data
public class RegisterRequest {
  private String email;
  private String name;
  private String password;
}
