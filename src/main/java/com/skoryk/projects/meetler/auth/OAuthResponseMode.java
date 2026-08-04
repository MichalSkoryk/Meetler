package com.skoryk.projects.meetler.auth;

public enum OAuthResponseMode {
  WEB_TOKENS("web_tokens"),
  MOBILE_CODE("mobile_code");

  private final String value;

  OAuthResponseMode(String value) {
    this.value = value;
  }

  public String value() {
    return value;
  }

  public static OAuthResponseMode from(String value) {
    if (value == null || value.isBlank() || WEB_TOKENS.value.equalsIgnoreCase(value)) {
      return WEB_TOKENS;
    }
    if (MOBILE_CODE.value.equalsIgnoreCase(value)) {
      return MOBILE_CODE;
    }
    throw new IllegalArgumentException("Unsupported OAuth response mode");
  }
}
