package com.skoryk.projects.meetler.group.groupInvite;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface GroupInviteRepository extends JpaRepository<GroupInvite, UUID> {

  Optional<GroupInvite> findByCode(String code);
}
