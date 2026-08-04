package com.skoryk.projects.meetler.auth;

import com.skoryk.projects.meetler.user.AppUser;

public record OAuthLoginResult(AppUser user, String returnUrl, OAuthResponseMode responseMode) {}
