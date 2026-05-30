package com.skoryk.projects.meetler.group;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "app_group")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Group {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @Column(nullable = false)
  private OffsetDateTime updatedAt;
}
