package com.skoryk.projects.meetler.notification.dto;

import com.skoryk.projects.meetler.notification.NotificationType;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationResponse {
  private UUID id;
  private NotificationType type;
  private String title;
  private String body;
  private UUID groupId;
  private UUID eventId;
  private boolean read;
  private OffsetDateTime readAt;
  private OffsetDateTime createdAt;
}
