package com.skoryk.projects.meetler.common.dto.auth;

import java.util.UUID;

public record SuccessfulRegisterResponse(UUID id, String email, String username, String role) {}
