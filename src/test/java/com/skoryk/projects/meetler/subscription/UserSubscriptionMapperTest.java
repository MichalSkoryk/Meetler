package com.skoryk.projects.meetler.subscription;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.subscription.dto.UserSubscriptionResponse;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class UserSubscriptionMapperTest {

  private final UserSubscriptionMapper mapper = Mappers.getMapper(UserSubscriptionMapper.class);

  @Test
  void mapsSubscriptionAndNestedPropertiesToResponse() {
    UUID subscriptionId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    UUID planId = UUID.randomUUID();
    OffsetDateTime startedAt = OffsetDateTime.parse("2026-08-01T10:15:30+02:00");
    OffsetDateTime expiresAt = OffsetDateTime.parse("2027-08-01T10:15:30+02:00");
    AppUser user = AppUser.builder().id(userId).build();
    SubscriptionPlan plan = SubscriptionPlan.builder().id(planId).code("PLUS").name("Plus").build();
    UserSubscription subscription =
        UserSubscription.builder()
            .id(subscriptionId)
            .user(user)
            .plan(plan)
            .status(SubscriptionStatus.ACTIVE)
            .startedAt(startedAt)
            .expiresAt(expiresAt)
            .build();

    UserSubscriptionResponse response = mapper.toResponse(subscription);

    assertThat(response.getId()).isEqualTo(subscriptionId);
    assertThat(response.getUserId()).isEqualTo(userId);
    assertThat(response.getPlanId()).isEqualTo(planId);
    assertThat(response.getPlanCode()).isEqualTo("PLUS");
    assertThat(response.getPlanName()).isEqualTo("Plus");
    assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
    assertThat(response.getStartedAt()).isEqualTo(startedAt);
    assertThat(response.getExpiresAt()).isEqualTo(expiresAt);
  }
}
