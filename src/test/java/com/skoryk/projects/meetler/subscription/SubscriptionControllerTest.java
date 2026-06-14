package com.skoryk.projects.meetler.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.subscription.dto.AssignUserSubscriptionRequest;
import com.skoryk.projects.meetler.subscription.dto.UserSubscriptionResponse;
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
class SubscriptionControllerTest {

  @Mock private UserSubscriptionService userSubscriptionService;

  @InjectMocks private SubscriptionController controller;

  @Test
  void assignSubscriptionPlanDelegatesToService() {
    UUID userId = UUID.randomUUID();
    AppUser requester = user();
    AssignUserSubscriptionRequest request = new AssignUserSubscriptionRequest();
    request.setPlanCode("PLUS");
    UserSubscriptionResponse serviceResponse =
        UserSubscriptionResponse.builder().userId(userId).planCode("PLUS").build();
    when(userSubscriptionService.assignPlan(userId, "PLUS", requester)).thenReturn(serviceResponse);

    ResponseEntity<UserSubscriptionResponse> response =
        controller.assignSubscriptionPlan(userId, request, requester);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(serviceResponse);
    verify(userSubscriptionService).assignPlan(userId, "PLUS", requester);
  }

  @Test
  void getActiveSubscriptionDelegatesToService() {
    UUID userId = UUID.randomUUID();
    AppUser requester = user();
    UserSubscriptionResponse serviceResponse =
        UserSubscriptionResponse.builder().userId(userId).planCode("FREE").build();
    when(userSubscriptionService.getActiveSubscription(userId, requester))
        .thenReturn(serviceResponse);

    ResponseEntity<UserSubscriptionResponse> response =
        controller.getActiveSubscription(userId, requester);

    assertThat(response.getStatusCode().value()).isEqualTo(200);
    assertThat(response.getBody()).isSameAs(serviceResponse);
    verify(userSubscriptionService).getActiveSubscription(userId, requester);
  }

  private AppUser user() {
    return AppUser.builder()
        .id(UUID.randomUUID())
        .email(UUID.randomUUID() + "@example.com")
        .role(AppUserRole.ADMIN)
        .build();
  }
}
