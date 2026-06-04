package com.skoryk.projects.meetler.auth.microsoft;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class MicrosoftTokenResponse {

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
