package com.skoryk.projects.meetler.group.invite;

import com.skoryk.projects.meetler.group.invite.dto.GroupInviteResponse;
import com.skoryk.projects.meetler.user.AppUser;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupInviteController implements GroupInviteApi {

  private final GroupInviteService inviteService;

  @Override
  public ResponseEntity<List<GroupInviteResponse>> listActiveInvites(UUID groupId, AppUser user) {
    return ResponseEntity.ok(inviteService.listActiveInvites(groupId, user));
  }

  @Override
  public ResponseEntity<GroupInviteResponse> createInvite(
      UUID groupId, InviteRequest request, AppUser user) {
    return ResponseEntity.ok(
        inviteService.createInvite(groupId, user, request.maxUses, request.expiresAt));
  }

  @Override
  public ResponseEntity<GroupInviteResponse> revokeInvite(UUID inviteId, AppUser user) {
    return ResponseEntity.ok(inviteService.revokeInvite(inviteId, user));
  }

  @Override
  public ResponseEntity<Void> joinWithCode(String code, AppUser user) {
    inviteService.joinWithCode(code, user);
    return ResponseEntity.ok().build();
  }

  @Data
  public static class InviteRequest {
    @Min(1)
    private Integer maxUses;

    private OffsetDateTime expiresAt;
  }
}
