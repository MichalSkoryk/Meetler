package com.skoryk.projects.meetler.group.invite;

import com.skoryk.projects.meetler.group.Group;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, UUID> {

  Optional<GroupInvite> findByCode(String code);

  List<GroupInvite> findByGroupOrderByCreatedAtDesc(Group group);
}
