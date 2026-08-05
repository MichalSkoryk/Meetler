package com.skoryk.projects.meetler.group.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.event.dto.GroupEventResponse;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class GroupEventMapperTest {

  private final GroupEventMapper mapper = Mappers.getMapper(GroupEventMapper.class);

  @Test
  void mapsEventAndParticipantsToResponse() {
    UUID groupId = UUID.randomUUID();
    UUID creatorId = UUID.randomUUID();
    UUID participantId = UUID.randomUUID();
    OffsetDateTime respondedAt = OffsetDateTime.parse("2026-08-05T15:00:00+02:00");
    AppUser creator = AppUser.builder().id(creatorId).build();
    AppUser participantUser =
        AppUser.builder().id(participantId).email("member@example.com").build();
    GroupEvent event =
        GroupEvent.builder()
            .id(UUID.randomUUID())
            .group(Group.builder().id(groupId).build())
            .createdBy(creator)
            .title("Rehearsal")
            .description("Main hall")
            .startsAt(OffsetDateTime.parse("2026-08-10T18:00:00+02:00"))
            .endsAt(OffsetDateTime.parse("2026-08-10T20:00:00+02:00"))
            .status(GroupEventStatus.CONFIRMED)
            .requiresConfirmation(true)
            .createdAt(OffsetDateTime.parse("2026-08-01T12:00:00+02:00"))
            .updatedAt(OffsetDateTime.parse("2026-08-02T12:00:00+02:00"))
            .build();
    GroupEventParticipant participant =
        GroupEventParticipant.builder()
            .groupEvent(event)
            .user(participantUser)
            .status(GroupEventParticipantStatus.ACCEPTED)
            .respondedAt(respondedAt)
            .build();

    GroupEventResponse response = mapper.toResponse(event, List.of(participant));

    assertThat(response.getId()).isEqualTo(event.getId());
    assertThat(response.getGroupId()).isEqualTo(groupId);
    assertThat(response.getCreatedByUserId()).isEqualTo(creatorId);
    assertThat(response.getStatus()).isEqualTo("CONFIRMED");
    assertThat(response.isRequiresConfirmation()).isTrue();
    assertThat(response.getParticipants())
        .singleElement()
        .satisfies(
            mappedParticipant -> {
              assertThat(mappedParticipant.getUserId()).isEqualTo(participantId);
              assertThat(mappedParticipant.getUserEmail()).isEqualTo("member@example.com");
              assertThat(mappedParticipant.getStatus()).isEqualTo("ACCEPTED");
              assertThat(mappedParticipant.getRespondedAt()).isEqualTo(respondedAt);
            });
  }
}
