package com.skoryk.projects.meetler.group.event;

import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

@Entity
@Table(
    name = "group_event_participant",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_group_event_participant_event_user",
          columnNames = {"group_event_id", "user_id"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupEventParticipant {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_event_id", nullable = false)
  private GroupEvent groupEvent;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private GroupEventParticipantStatus status;

  private OffsetDateTime respondedAt;

  @CreationTimestamp
  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
