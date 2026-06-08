package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.group.invite.GroupInviteService;
import com.skoryk.projects.meetler.group.member.dto.GroupAvailabilityTemplateResponse;
import com.skoryk.projects.meetler.group.member.dto.GroupMemberResponse;
import com.skoryk.projects.meetler.group.member.dto.SelectGroupAvailabilityTemplateRequest;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupMemberController implements GroupMemberApi {

  private final GroupMemberService memberService;
  private final GroupInviteService inviteService;

  @Override
  public ResponseEntity<Void> joinGroup(UUID groupId, String code, AppUser user) {
    inviteService.joinGroupWithCode(groupId, code, user);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> leaveGroup(UUID groupId, AppUser user) {
    memberService.removeMember(groupId, user);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<List<GroupMemberResponse>> listMembers(UUID groupId) {
    return ResponseEntity.ok(memberService.getGroupMembers(groupId));
  }

  @Override
  public ResponseEntity<GroupAvailabilityTemplateResponse> getMyAvailabilityTemplate(
      UUID groupId, AppUser user) {
    return ResponseEntity.ok(memberService.getAvailabilityTemplate(groupId, user));
  }

  @Override
  public ResponseEntity<GroupAvailabilityTemplateResponse> selectMyAvailabilityTemplate(
      UUID groupId, SelectGroupAvailabilityTemplateRequest request, AppUser user) {
    return ResponseEntity.ok(
        memberService.selectAvailabilityTemplate(
            groupId, user, request.getAvailabilityTemplateId()));
  }

  @Override
  public ResponseEntity<GroupAvailabilityTemplateResponse> clearMyAvailabilityTemplate(
      UUID groupId, AppUser user) {
    return ResponseEntity.ok(memberService.clearAvailabilityTemplate(groupId, user));
  }
}
