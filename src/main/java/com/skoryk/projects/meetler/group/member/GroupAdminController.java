package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.user.AppUser;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class GroupAdminController implements GroupAdminApi {

  private final GroupAdminService groupAdminService;

  @Override
  public ResponseEntity<Void> promoteToAdmin(UUID groupId, UUID userId, AppUser requester) {
    groupAdminService.promoteToAdmin(groupId, userId, requester);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> demoteAdmin(UUID groupId, UUID userId, AppUser requester) {
    groupAdminService.demoteAdmin(groupId, userId, requester);
    return ResponseEntity.ok().build();
  }

  @Override
  public ResponseEntity<Void> removeMember(UUID groupId, UUID userId, AppUser requester) {
    groupAdminService.removeMember(groupId, userId, requester);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<Void> transferOwnership(UUID groupId, UUID newOwnerId, AppUser requester) {
    groupAdminService.transferOwnership(groupId, newOwnerId, requester);
    return ResponseEntity.ok().build();
  }
}
