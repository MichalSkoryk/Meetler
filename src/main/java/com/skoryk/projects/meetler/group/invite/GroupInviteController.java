package com.skoryk.projects.meetler.group.invite;

import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Group Invites")
@RestController
@RequestMapping("/api/invites")
@RequiredArgsConstructor
public class GroupInviteController {

  private final GroupInviteService inviteService;

  @PostMapping("/{groupId}")
  public ResponseEntity<Map<String, String>> createInvite(
      @PathVariable UUID groupId,
      @Valid @RequestBody InviteRequest request,
      @AuthenticationPrincipal AppUser user) {
    String code = inviteService.createInvite(groupId, user, request.maxUses, request.expiresAt);
    return ResponseEntity.ok(Map.of("code", code));
  }

  @PostMapping("/join/{code}")
  public ResponseEntity<Void> joinWithCode(
      @PathVariable String code, @AuthenticationPrincipal AppUser user) {
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
