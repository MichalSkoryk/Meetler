package com.skoryk.projects.meetler.group.member;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "group_member")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupMember {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private Group group;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private GroupRole role;

  @Column(nullable = false)
  private OffsetDateTime joinedAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "availability_template_id")
  private AvailabilityTemplate availabilityTemplate;
}
