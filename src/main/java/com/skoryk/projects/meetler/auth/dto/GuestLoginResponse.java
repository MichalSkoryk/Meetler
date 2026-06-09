package com.skoryk.projects.meetler.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class GuestLoginResponse {

  private String message;
  private String loginLink;
}
