package com.skoryk.projects.meetler.group;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.group.dto.GroupEventSummaryResponse;
import com.skoryk.projects.meetler.group.dto.GroupResponse;
import com.skoryk.projects.meetler.group.event.GroupEvent;
import com.skoryk.projects.meetler.group.event.GroupEventParticipant;
import com.skoryk.projects.meetler.group.event.GroupEventParticipantStatus;
import com.skoryk.projects.meetler.group.event.GroupEventStatus;
import com.skoryk.projects.meetler.group.member.GroupMember;
import com.skoryk.projects.meetler.group.member.GroupRole;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class GroupMapperTest {

  private final GroupMapper mapper = Mappers.getMapper(GroupMapper.class);

  @Test
  void mapsGroupMembershipAndEventSummaries() {
    UUID groupId = UUID.randomUUID();
    OffsetDateTime createdAt = OffsetDateTime.parse("2026-08-01T10:00:00+02:00");
    OffsetDateTime updatedAt = OffsetDateTime.parse("2026-08-02T10:00:00+02:00");
    Group group =
        Group.builder()
            .id(groupId)
            .name("Orchestra")
            .eventRequiresConfirmation(true)
            .createdAt(createdAt)
            .updatedAt(updatedAt)
            .build();
    GroupMember membership =
        GroupMember.builder()
            .group(group)
            .user(AppUser.builder().id(UUID.randomUUID()).build())
            .role(GroupRole.ADMIN)
            .availabilityTemplate(AvailabilityTemplate.builder().id(UUID.randomUUID()).build())
            .build();
    GroupEventSummaryResponse nextEvent =
        GroupEventSummaryResponse.builder().id(UUID.randomUUID()).title("Rehearsal").build();

    GroupResponse response = mapper.toResponse(membership, 12, 3, nextEvent, nextEvent);

    assertThat(response.getId()).isEqualTo(groupId);
    assertThat(response.getName()).isEqualTo("Orchestra");
    assertThat(response.getRole()).isEqualTo("ADMIN");
    assertThat(response.getMemberCount()).isEqualTo(12);
    assertThat(response.isHasAvailabilityTemplate()).isTrue();
    assertThat(response.getPendingResponseCount()).isEqualTo(3);
    assertThat(response.getNextEvent()).isSameAs(nextEvent);
    assertThat(response.getNextPendingEvent()).isSameAs(nextEvent);
    assertThat(response.isEventRequiresConfirmation()).isTrue();
    assertThat(response.getCreatedAt()).isEqualTo(createdAt);
    assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
  }

  @Test
  void mapsEventSummaryAndHandlesMissingParticipant() {
    GroupEvent event =
        GroupEvent.builder()
            .id(UUID.randomUUID())
            .title("Concert")
            .startsAt(OffsetDateTime.parse("2026-09-01T18:00:00+02:00"))
            .endsAt(OffsetDateTime.parse("2026-09-01T20:00:00+02:00"))
            .status(GroupEventStatus.PENDING_CONFIRMATION)
            .requiresConfirmation(true)
            .build();
    GroupEventParticipant participant =
        GroupEventParticipant.builder().status(GroupEventParticipantStatus.ACCEPTED).build();

    GroupEventSummaryResponse response = mapper.toEventSummary(event, participant);
    GroupEventSummaryResponse responseWithoutParticipant = mapper.toEventSummary(event, null);

    assertThat(response.getId()).isEqualTo(event.getId());
    assertThat(response.getTitle()).isEqualTo("Concert");
    assertThat(response.getStatus()).isEqualTo("PENDING_CONFIRMATION");
    assertThat(response.isRequiresConfirmation()).isTrue();
    assertThat(response.getMyResponseStatus()).isEqualTo("ACCEPTED");
    assertThat(responseWithoutParticipant.getMyResponseStatus()).isNull();
  }
}
