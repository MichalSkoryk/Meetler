package com.skoryk.projects.meetler.auth;

import com.skoryk.projects.meetler.auth.dto.AuthResponse;

public record OAuthLoginResult(AuthResponse authResponse, String returnUrl) {}
