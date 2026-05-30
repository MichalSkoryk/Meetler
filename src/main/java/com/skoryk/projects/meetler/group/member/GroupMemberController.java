package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Group Members")
@RestController
@RequestMapping("/api/groups/{groupId}/members")
@RequiredArgsConstructor
public class GroupMemberController {

  private final GroupMemberService memberService;

  @PostMapping("/join")
  public ResponseEntity<Void> joinGroup(
      @PathVariable UUID groupId, @AuthenticationPrincipal AppUser user) {
    memberService.addMember(groupId, user, GroupRole.MEMBER);
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
