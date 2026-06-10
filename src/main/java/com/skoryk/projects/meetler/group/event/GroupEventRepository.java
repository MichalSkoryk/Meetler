package com.skoryk.projects.meetler.group.event;

import com.skoryk.projects.meetler.group.Group;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface GroupEventRepository extends JpaRepository<GroupEvent, UUID> {

  Optional<GroupEvent> findByIdAndGroup(UUID id, Group group);

  @Query(
      """
      select e
      from GroupEvent e
      where e.group = :group
        and e.endsAt > :from
        and e.startsAt < :to
      order by e.startsAt asc
      """)
  List<GroupEvent> findByGroupOverlapping(Group group, OffsetDateTime from, OffsetDateTime to);
}
