package com.skoryk.projects.meetler.group.event;

import com.skoryk.projects.meetler.user.AppUser;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface GroupEventParticipantRepository
    extends JpaRepository<GroupEventParticipant, UUID> {

  List<GroupEventParticipant> findByGroupEvent(GroupEvent groupEvent);

  Optional<GroupEventParticipant> findByGroupEventAndUser(GroupEvent groupEvent, AppUser user);

  @Modifying
  @Query(
      """
          update GroupEventParticipant p
          set p.status = :groupEventParticipantStatus,
                    p.respondedAt = :respondedAt
          where p.groupEvent = :groupEvent
          """)
  void setParticipantStateInEvent(
      GroupEvent groupEvent,
      GroupEventParticipantStatus groupEventParticipantStatus,
      OffsetDateTime respondedAt);
}
