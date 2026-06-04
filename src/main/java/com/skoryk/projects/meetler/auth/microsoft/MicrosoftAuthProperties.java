package com.skoryk.projects.meetler.auth.microsoft;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.microsoft.oauth")
public class MicrosoftAuthProperties {

  private String clientId;
  private String clientSecret;
  private String redirectUri;
  private String authorizationUri =
      "https://login.microsoftonline.com/consumers/oauth2/v2.0/authorize";
  private String tokenUri = "https://login.microsoftonline.com/consumers/oauth2/v2.0/token";
  private String userInfoUri = "https://graph.microsoft.com/oidc/userinfo";
  private List<String> scopes = List.of("openid", "email", "profile", "User.Read");
}
