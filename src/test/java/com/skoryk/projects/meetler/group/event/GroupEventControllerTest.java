package com.skoryk.projects.meetler.group.event;

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
class GroupEventControllerTest {

  @Mock private GroupEventService groupEventService;

  @InjectMocks private GroupEventController controller;

  @Test
  void cancelEventDelegatesToServiceAndReturnsNoContent() {
    UUID groupId = UUID.randomUUID();
    UUID eventId = UUID.randomUUID();
    AppUser user = user();

    ResponseEntity<Void> response = controller.cancelEvent(groupId, eventId, user);

    assertThat(response.getStatusCode().value()).isEqualTo(204);
    assertThat(response.getBody()).isNull();
    verify(groupEventService).cancelEvent(groupId, eventId, user);
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email(UUID.randomUUID() + "@example.com")
        .role(AppUserRole.USER)
        .build();
  }
}
