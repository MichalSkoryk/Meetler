package com.skoryk.projects.meetler.user;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.user.dto.UserResponse;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class AppUserMapperTest {

  private final AppUserMapper mapper = Mappers.getMapper(AppUserMapper.class);

  @Test
  void mapsPublicUserFields() {
    UUID userId = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-01T12:00:00+02:00");
    OffsetDateTime upgradedAt = OffsetDateTime.parse("2026-08-02T12:00:00+02:00");
    AppUser user =
        AppUser.builder()
            .id(userId)
            .email("user@example.com")
            .name("User")
            .role(AppUserRole.USER)
            .passwordHash("secret-hash")
            .createdAt(createdAt)
            .upgradedAt(upgradedAt)
            .build();

    UserResponse response = mapper.toResponse(user);

    assertThat(response.getId()).isEqualTo(userId);
    assertThat(response.getEmail()).isEqualTo("user@example.com");
    assertThat(response.getName()).isEqualTo("User");
    assertThat(response.getRole()).isEqualTo(AppUserRole.USER);
    assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    assertThat(response.getUpgradedAt()).isEqualTo(upgradedAt);
  }
}
