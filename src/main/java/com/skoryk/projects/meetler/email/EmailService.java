package com.skoryk.projects.meetler.email;

import java.time.OffsetDateTime;

public interface EmailService {

  void sendGuestLoginLink(String recipientEmail, String loginLink, OffsetDateTime expiresAt);

  void sendPasswordResetLink(String recipientEmail, String resetLink, OffsetDateTime expiresAt);
}
