package com.skoryk.projects.meetler.notification;

import com.skoryk.projects.meetler.user.AppUser;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

  Page<Notification> findByUserOrderByCreatedAtDesc(AppUser user, Pageable pageable);

  Page<Notification> findByUserAndReadAtIsNullOrderByCreatedAtDesc(AppUser user, Pageable pageable);

  long countByUserAndReadAtIsNull(AppUser user);

  @Modifying
  @Query(
      "update Notification n set n.readAt = current_timestamp where n.user = :user and n.readAt is null")
  void markAllUnreadAsRead(AppUser user);
}
