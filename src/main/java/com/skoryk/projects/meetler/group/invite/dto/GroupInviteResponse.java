package com.skoryk.projects.meetler.group.invite.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@Schema(description = "Group invite code.")
public class GroupInviteResponse {

  @Schema(description = "Invite identifier.", example = "6bb620c7-4fbc-498c-bdd3-4ba4a7c84029")
  private UUID id;

  @Schema(description = "Invite code used to join the group.", example = "a1b2c3d4")
  private String code;

  @Schema(description = "When this invite expires. Null means it does not expire.")
  private OffsetDateTime expiresAt;

  @Schema(description = "Maximum number of times this invite can be used. Null means unlimited.")
  private Integer maxUses;

  @Schema(description = "How many times this invite has already been used.", example = "2")
  private Integer uses;

  @Schema(description = "When this invite was created.")
  private OffsetDateTime createdAt;

  @Schema(description = "When this invite was revoked. Null means it was not revoked.")
  private OffsetDateTime revokedAt;

  @Schema(description = "User who revoked this invite. Null means it was not revoked.")
  private UUID revokedByUserId;
}
