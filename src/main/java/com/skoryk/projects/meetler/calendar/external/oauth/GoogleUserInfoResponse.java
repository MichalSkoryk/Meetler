package com.skoryk.projects.meetler.calendar.external.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleUserInfoResponse {

  private String sub;
  private String email;
  private String name;

  @JsonProperty("email_verified")
  private Boolean emailVerified;
}
