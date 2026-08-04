package com.skoryk.projects.meetler.notification.delivery;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NotificationDeliveryRepository extends JpaRepository<NotificationDelivery, UUID> {

  @Query(
      """
      select delivery.id from NotificationDelivery delivery
      where delivery.status = com.skoryk.projects.meetler.notification.delivery.NotificationDeliveryStatus.PENDING
        and (delivery.nextAttemptAt is null or delivery.nextAttemptAt <= :now)
      order by delivery.createdAt
      """)
  List<UUID> findReadyDeliveryIds(@Param("now") OffsetDateTime now, Pageable pageable);
}
