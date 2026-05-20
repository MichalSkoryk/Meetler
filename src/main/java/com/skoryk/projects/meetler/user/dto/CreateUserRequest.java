package com.skoryk.projects.meetler.user.dto;

import com.skoryk.projects.meetler.user.AuthProvider;
import lombok.Data;

@Data
public class CreateUserRequest {
  private String email;
  private AuthProvider authProvider;
}
