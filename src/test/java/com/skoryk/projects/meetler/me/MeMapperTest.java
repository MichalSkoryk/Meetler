package com.skoryk.projects.meetler.me;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.subscription.SubscriptionUsageResponse;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class MeMapperTest {

  private final MeMapper mapper = Mappers.getMapper(MeMapper.class);

  @Test
  void mapsUserAndDerivesPasswordPresence() {
    AppUser user =
        AppUser.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .name("User")
            .role(AppUserRole.USER)
            .passwordHash("encoded-password")
            .build();

    MeUserResponse response = mapper.toUserResponse(user);

    assertThat(response.getId()).isEqualTo(user.getId());
    assertThat(response.getEmail()).isEqualTo("user@example.com");
    assertThat(response.getName()).isEqualTo("User");
    assertThat(response.getRole()).isEqualTo(AppUserRole.USER);
    assertThat(response.isHasPassword()).isTrue();
    assertThat(mapper.toUserResponse(AppUser.builder().passwordHash("  ").build()).isHasPassword())
        .isFalse();
  }

  @Test
  void mapsBootstrapAndConnectionFlags() {
    MeUserResponse user = MeUserResponse.builder().id(UUID.randomUUID()).build();
    SubscriptionUsageResponse subscription =
        SubscriptionUsageResponse.builder().planCode("PLUS").build();
    ConnectedCalendarsResponse calendars = mapper.toConnectedCalendarsResponse(true, false);
    ConnectedLoginMethodsResponse loginMethods =
        mapper.toConnectedLoginMethodsResponse(true, true, false);

    MeBootstrapResponse response =
        mapper.toBootstrapResponse(user, subscription, calendars, loginMethods);

    assertThat(response.getUser()).isSameAs(user);
    assertThat(response.getSubscription()).isSameAs(subscription);
    assertThat(response.getConnectedCalendars()).isSameAs(calendars);
    assertThat(response.getConnectedLoginMethods()).isSameAs(loginMethods);
    assertThat(calendars.isGoogle()).isTrue();
    assertThat(calendars.isMicrosoft()).isFalse();
    assertThat(loginMethods.isPassword()).isTrue();
    assertThat(loginMethods.isGoogle()).isTrue();
    assertThat(loginMethods.isMicrosoft()).isFalse();
  }
}
