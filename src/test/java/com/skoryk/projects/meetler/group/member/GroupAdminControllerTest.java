package com.skoryk.projects.meetler.group.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class GroupAdminControllerTest {

  @Mock private GroupAdminService groupAdminService;

  @InjectMocks private GroupAdminController controller;

  @Test
  void promoteDelegatesToService() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    AppUser requester = user();

    ResponseEntity<Void> response = controller.promoteToAdmin(groupId, userId, requester);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    verify(groupAdminService).promoteToAdmin(groupId, userId, requester);
  }

  @Test
  void removeMemberDelegatesToService() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    AppUser requester = user();

    ResponseEntity<Void> response = controller.removeMember(groupId, userId, requester);

    assertThat(response.getStatusCode().value()).isEqualTo(204);
    verify(groupAdminService).removeMember(groupId, userId, requester);
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email(UUID.randomUUID() + "@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
