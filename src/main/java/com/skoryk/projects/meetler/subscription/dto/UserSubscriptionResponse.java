package com.skoryk.projects.meetler.subscription.dto;

import com.skoryk.projects.meetler.subscription.SubscriptionStatus;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserSubscriptionResponse {

  private UUID id;
  private UUID userId;
  private UUID planId;
  private String planCode;
  private String planName;
  private SubscriptionStatus status;
  private OffsetDateTime startedAt;
  private OffsetDateTime expiresAt;
}
