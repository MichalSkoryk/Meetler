package com.skoryk.projects.meetler.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import com.skoryk.projects.meetler.group.event.GroupEventParticipant;
import com.skoryk.projects.meetler.notification.delivery.*;
import com.skoryk.projects.meetler.notification.device.*;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

  @Mock private NotificationRepository notificationRepository;
  @Mock private UserDeviceRepository userDeviceRepository;
  @Mock private NotificationDeliveryRepository deliveryRepository;
  @Mock private ApplicationEventPublisher eventPublisher;
  @Spy private NotificationMapper notificationMapper = Mappers.getMapper(NotificationMapper.class);

  @InjectMocks private NotificationService notificationService;

  @Test
  void notifyGroupEventCreatedCreatesConfirmationNotificationForOtherParticipants() {
    AppUser creator = user("creator@example.com");
    AppUser member = user("member@example.com");
    Group group = group();
    GroupEvent event = event(group, creator, true);
    UserDevice device = device(member);

    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(
            invocation -> {
              Notification notification = invocation.getArgument(0);
              notification.setId(UUID.randomUUID());
              return notification;
            });
    when(userDeviceRepository.findByUserAndEnabledTrueAndRevokedAtIsNull(member))
        .thenReturn(List.of(device));
    when(deliveryRepository.save(any(NotificationDelivery.class)))
        .thenAnswer(
            invocation -> {
              NotificationDelivery delivery = invocation.getArgument(0);
              delivery.setId(UUID.randomUUID());
              return delivery;
            });

    notificationService.notifyGroupEventCreated(
        event, List.of(participant(event, creator), participant(event, member)));

    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository).save(notificationCaptor.capture());
    Notification notification = notificationCaptor.getValue();
    assertThat(notification.getUser()).isEqualTo(member);
    assertThat(notification.getType())
        .isEqualTo(NotificationType.GROUP_EVENT_CONFIRMATION_REQUIRED);
    assertThat(notification.getGroupId()).isEqualTo(group.getId());
    assertThat(notification.getEventId()).isEqualTo(event.getId());

    ArgumentCaptor<NotificationDelivery> deliveryCaptor =
        ArgumentCaptor.forClass(NotificationDelivery.class);
    verify(deliveryRepository).save(deliveryCaptor.capture());
    assertThat(deliveryCaptor.getValue().getStatus()).isEqualTo(NotificationDeliveryStatus.PENDING);
    assertThat(deliveryCaptor.getValue().getSentAt()).isNull();
    verify(eventPublisher).publishEvent(any(NotificationDeliveryRequested.class));
  }

  @Test
  void notifyGroupEventConfirmedCreatesNotificationForAllParticipants() {
    AppUser creator = user("creator@example.com");
    AppUser member = user("member@example.com");
    Group group = group();
    GroupEvent event = event(group, creator, true);

    when(notificationRepository.save(any(Notification.class)))
        .thenAnswer(
            invocation -> {
              Notification notification = invocation.getArgument(0);
              notification.setId(UUID.randomUUID());
              return notification;
            });

    notificationService.notifyGroupEventConfirmed(
        event, List.of(participant(event, creator), participant(event, member)));

    ArgumentCaptor<Notification> notificationCaptor = ArgumentCaptor.forClass(Notification.class);
    verify(notificationRepository, times(2)).save(notificationCaptor.capture());
    assertThat(notificationCaptor.getAllValues())
        .allSatisfy(
            notification -> {
              assertThat(notification.getType()).isEqualTo(NotificationType.GROUP_EVENT_CONFIRMED);
              assertThat(notification.getTitle()).isEqualTo("Group event confirmed");
              assertThat(notification.getBody()).isEqualTo("Dinner was confirmed.");
              assertThat(notification.getGroupId()).isEqualTo(group.getId());
              assertThat(notification.getEventId()).isEqualTo(event.getId());
            });
  }

  @Test
  void countUnreadReturnsRepositoryCount() {
    AppUser user = user("member@example.com");
    when(notificationRepository.countByUserAndReadAtIsNull(user)).thenReturn(7L);

    assertThat(notificationService.countUnread(user).getUnreadCount()).isEqualTo(7L);
  }

  private GroupEventParticipant participant(GroupEvent event, AppUser user) {
    return GroupEventParticipant.builder().groupEvent(event).user(user).build();
  }

  private UserDevice device(AppUser user) {
    return UserDevice.builder()
        .id(UUID.randomUUID())
        .user(user)
        .platform(UserDevicePlatform.ANDROID)
        .provider(UserDeviceProvider.FCM)
        .token("token")
        .enabled(true)
        .build();
  }

  private GroupEvent event(Group group, AppUser creator, boolean requiresConfirmation) {
    return GroupEvent.builder()
        .id(UUID.randomUUID())
        .group(group)
        .createdBy(creator)
        .title("Dinner")
        .startsAt(OffsetDateTime.parse("2026-06-10T18:00:00+02:00"))
        .endsAt(OffsetDateTime.parse("2026-06-10T19:00:00+02:00"))
        .requiresConfirmation(requiresConfirmation)
        .build();
  }

  private Group group() {
    return Group.builder().id(UUID.randomUUID()).name("Friends").build();
  }

  private AppUser user(String email) {
    return AppUser.builder().id(UUID.randomUUID()).email(email).role(AppUserRole.USER).build();
  }
}
