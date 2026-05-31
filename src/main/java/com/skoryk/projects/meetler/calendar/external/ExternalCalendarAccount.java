package com.skoryk.projects.meetler.calendar.external;

import com.skoryk.projects.meetler.calendar.CalendarProvider;
import com.skoryk.projects.meetler.user.AppUser;
import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

@Entity
@Table(
    name = "external_calendar_account",
    uniqueConstraints = {
      @UniqueConstraint(
          name = "uq_external_calendar_account_user_provider_external",
          columnNames = {"user_id", "provider", "external_account_id"})
    })
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExternalCalendarAccount {

  @Id @GeneratedValue private UUID id;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id", nullable = false)
  private AppUser user;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private CalendarProvider provider;

  @Column(name = "external_account_id", nullable = false)
  private String externalAccountId;

  @Column(name = "account_email")
  private String accountEmail;

  @Column(columnDefinition = "TEXT")
  private String scopes;

  @Column(name = "access_token_encrypted", nullable = false, columnDefinition = "TEXT")
  private String accessTokenEncrypted;

  @Column(name = "refresh_token_encrypted", columnDefinition = "TEXT")
  private String refreshTokenEncrypted;

  @Column(name = "token_type")
  private String tokenType;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  @Column(name = "last_synced_at")
  private OffsetDateTime lastSyncedAt;

  @Column(name = "revoked_at")
  private OffsetDateTime revokedAt;

  @CreationTimestamp
  @Column(nullable = false)
  private OffsetDateTime createdAt;

  @UpdateTimestamp
  @Column(nullable = false)
  private OffsetDateTime updatedAt;
}
