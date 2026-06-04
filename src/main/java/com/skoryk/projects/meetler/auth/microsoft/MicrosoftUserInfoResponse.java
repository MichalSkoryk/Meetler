package com.skoryk.projects.meetler.auth.microsoft;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MicrosoftUserInfoResponse {

  private String sub;
  private String email;
  private String name;

  @JsonProperty("preferred_username")
  private String preferredUsername;

  private String upn;

  public String resolveEmail() {
    if (email != null && !email.isBlank()) {
      return email;
    }
    if (preferredUsername != null && !preferredUsername.isBlank()) {
      return preferredUsername;
    }
    return upn;
  }
}
