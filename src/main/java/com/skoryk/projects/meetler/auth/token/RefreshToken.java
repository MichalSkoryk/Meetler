package com.skoryk.projects.meetler.auth.token;

import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;

@Entity
@Table(name = "refresh_token")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefreshToken {

  @Id @GeneratedValue private UUID id;

  @Column(nullable = false, unique = true)
  private String token;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Column(nullable = false)
  private OffsetDateTime expiresAt;

  @Column(nullable = false)
  private boolean revoked;
}
