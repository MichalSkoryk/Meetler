package com.skoryk.projects.meetler.user.dto;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

  private UUID id;
  private String email;
  private String name;
  private AppUserRole role;
  private OffsetDateTime createdAt;
  private OffsetDateTime upgradedAt;
  private OffsetDateTime deletedAt;

  public static UserResponse from(AppUser appUser) {
    return UserResponse.builder()
        .id(appUser.getId())
        .email(appUser.getEmail())
        .name(appUser.getName())
        .role(appUser.getRole())
        .createdAt(appUser.getCreatedAt())
        .upgradedAt(appUser.getUpgradedAt())
        .deletedAt(appUser.getDeletedAt())
        .build();
  }
}
