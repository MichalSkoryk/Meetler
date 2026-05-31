package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.group.invite.GroupInviteService;
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotBlank;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Group Members")
@Validated
@RestController
@RequestMapping("/api/groups/{groupId}/members")
@RequiredArgsConstructor
public class GroupMemberController {

  private final GroupMemberService memberService;
  private final GroupInviteService inviteService;

  @PostMapping("/join")
  public ResponseEntity<Void> joinGroup(
      @PathVariable UUID groupId,
      @NotBlank @RequestParam String code,
      @AuthenticationPrincipal AppUser user) {
    inviteService.joinGroupWithCode(groupId, code, user);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/leave")
  public ResponseEntity<Void> leaveGroup(
      @PathVariable UUID groupId, @AuthenticationPrincipal AppUser user) {
    memberService.removeMember(groupId, user);
    return ResponseEntity.noContent().build();
  }

  @GetMapping
  public ResponseEntity<?> listMembers(@PathVariable UUID groupId) {
    return ResponseEntity.ok(memberService.getGroupMembers(groupId));
  }
}
