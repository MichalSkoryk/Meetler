package com.skoryk.projects.meetler.group.groupInvite;

import com.skoryk.projects.meetler.group.Group;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "group_invite")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GroupInvite {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "group_id", nullable = false)
  private Group group;

  @Column(nullable = false, unique = true)
  private String code;

  private OffsetDateTime expiresAt;

  private Integer maxUses;

  @Column(nullable = false)
  private Integer uses;

  @Column(nullable = false)
  private OffsetDateTime createdAt;
}
