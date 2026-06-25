package com.skoryk.projects.meetler.notification.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UnreadNotificationCountResponse {
  private long unreadCount;
}
