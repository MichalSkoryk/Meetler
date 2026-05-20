package com.skoryk.projects.meetler.auth.dto;

import lombok.Data;

@Data
public class RefreshTokenRequest {
  private String refreshToken;
}
