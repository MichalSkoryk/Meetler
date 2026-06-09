package com.skoryk.projects.meetler.availability.repository;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.user.AppUser;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface AvailabilityTemplateRepository extends JpaRepository<AvailabilityTemplate, UUID> {

  List<AvailabilityTemplate> findByUserOrderByCreatedAtDesc(AppUser user);

  Page<AvailabilityTemplate> findByUserOrderByCreatedAtDesc(AppUser user, Pageable pageable);

  Optional<AvailabilityTemplate> findByIdAndUser(UUID id, AppUser user);

  boolean existsByUser(AppUser user);

  @Modifying
  @Query("update AvailabilityTemplate t set t.isDefault = false where t.user = :user")
  void clearDefaultForUser(AppUser user);
}
