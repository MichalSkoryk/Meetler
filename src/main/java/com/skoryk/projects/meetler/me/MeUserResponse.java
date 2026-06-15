package com.skoryk.projects.meetler.me;

import com.skoryk.projects.meetler.user.AppUserRole;
import java.util.UUID;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MeUserResponse {
  private UUID id;
  private String email;
  private String name;
  private AppUserRole role;
  private boolean hasPassword;
}
