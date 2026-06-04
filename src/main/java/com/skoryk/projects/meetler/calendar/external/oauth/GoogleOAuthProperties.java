package com.skoryk.projects.meetler.calendar.external.oauth;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "external-calendar.google.oauth")
public class GoogleOAuthProperties {

  private String clientId;
  private String clientSecret;
  private String redirectUri;
  private String authorizationUri = "https://accounts.google.com/o/oauth2/v2/auth";
  private String tokenUri = "https://oauth2.googleapis.com/token";
  private String userInfoUri = "https://openidconnect.googleapis.com/v1/userinfo";
  private List<String> scopes =
      List.of(
          "openid",
          "email",
          "profile",
          "https://www.googleapis.com/auth/calendar.readonly",
          "https://www.googleapis.com/auth/calendar.events");
}
