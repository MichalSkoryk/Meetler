package com.skoryk.projects.meetler.user.dto;

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
}
