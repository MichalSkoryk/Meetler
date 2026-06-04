package com.skoryk.projects.meetler.calendar.external.oauth;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class GoogleTokenResponse {

  @JsonProperty("access_token")
  private String accessToken;

  @JsonProperty("refresh_token")
  private String refreshToken;

  @JsonProperty("expires_in")
  private Long expiresIn;

  @JsonProperty("token_type")
  private String tokenType;

  private String scope;
}
