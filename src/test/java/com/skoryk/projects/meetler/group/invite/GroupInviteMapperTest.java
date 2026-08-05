package com.skoryk.projects.meetler.group.invite;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.group.invite.dto.GroupInviteResponse;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class GroupInviteMapperTest {

  private final GroupInviteMapper mapper = Mappers.getMapper(GroupInviteMapper.class);

  @Test
  void mapsInviteAndRevokingUser() {
    UUID revokedById = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-01T12:00:00+02:00");
    OffsetDateTime expiresAt = OffsetDateTime.parse("2026-08-08T12:00:00+02:00");
    OffsetDateTime revokedAt = OffsetDateTime.parse("2026-08-02T12:00:00+02:00");
    GroupInvite invite =
        GroupInvite.builder()
            .id(UUID.randomUUID())
            .code("invite-code")
            .expiresAt(expiresAt)
            .maxUses(10)
            .uses(2)
            .createdAt(createdAt)
            .revokedAt(revokedAt)
            .revokedBy(AppUser.builder().id(revokedById).build())
            .build();

    GroupInviteResponse response = mapper.toResponse(invite);

    assertThat(response.getId()).isEqualTo(invite.getId());
    assertThat(response.getCode()).isEqualTo("invite-code");
    assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
    assertThat(response.getMaxUses()).isEqualTo(10);
    assertThat(response.getUses()).isEqualTo(2);
    assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    assertThat(response.getRevokedAt()).isEqualTo(revokedAt);
    assertThat(response.getRevokedByUserId()).isEqualTo(revokedById);
  }

  @Test
  void mapsMissingRevokingUserToNull() {
    assertThat(mapper.toResponse(GroupInvite.builder().build()).getRevokedByUserId()).isNull();
  }
}
