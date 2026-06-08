package com.skoryk.projects.meetler.group;

import com.skoryk.projects.meetler.availability.dto.ResolvedAvailabilityWindowResponse;
import com.skoryk.projects.meetler.availability.model.AvailabilityBlockStatus;
import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.availability.resolver.AvailabilityTemplateResolver;
import com.skoryk.projects.meetler.group.dto.GroupAvailabilitySlotResponse;
import com.skoryk.projects.meetler.group.member.GroupMember;
import com.skoryk.projects.meetler.group.member.GroupMemberRepository;
import com.skoryk.projects.meetler.group.member.GroupPermissionService;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GroupAvailabilityService {

  private static final Duration SLOT_DURATION = Duration.ofMinutes(15);
  private static final int MAX_GRID_DAYS = 31;

  private final GroupRepository groupRepository;
  private final GroupMemberRepository memberRepository;
  private final GroupPermissionService permissionService;
  private final AvailabilityTemplateResolver availabilityTemplateResolver;

  @Transactional(readOnly = true)
  public List<GroupAvailabilitySlotResponse> getAvailabilityGrid(
      UUID groupId, AppUser requester, OffsetDateTime from, OffsetDateTime to) {
    validateRange(from, to);

    Group group =
        groupRepository
            .findById(groupId)
            .orElseThrow(() -> new IllegalArgumentException("Group not found"));

    if (!permissionService.isMember(group, requester)) {
      throw new IllegalArgumentException("Not allowed");
    }

    List<GroupMember> members = memberRepository.findByGroup(group);
    List<MemberResolvedAvailability> memberAvailability =
        members.stream().map(member -> resolveMemberAvailability(member, from, to)).toList();

    List<GroupAvailabilitySlotResponse> slots = new ArrayList<>();
    for (OffsetDateTime slotStart = from;
        !slotStart.plus(SLOT_DURATION).isAfter(to);
        slotStart = slotStart.plus(SLOT_DURATION)) {
      OffsetDateTime slotEnd = slotStart.plus(SLOT_DURATION);
      slots.add(toSlotResponse(slotStart, slotEnd, memberAvailability));
    }

    return slots;
  }

  private MemberResolvedAvailability resolveMemberAvailability(
      GroupMember member, OffsetDateTime from, OffsetDateTime to) {
    AvailabilityTemplate template = member.getAvailabilityTemplate();
    if (template == null) {
      return new MemberResolvedAvailability(member.getUser().getId(), null);
    }

    return new MemberResolvedAvailability(
        member.getUser().getId(), availabilityTemplateResolver.resolve(template, from, to));
  }

  private GroupAvailabilitySlotResponse toSlotResponse(
      OffsetDateTime slotStart,
      OffsetDateTime slotEnd,
      List<MemberResolvedAvailability> memberAvailability) {
    List<UUID> availableUserIds = new ArrayList<>();
    List<UUID> busyUserIds = new ArrayList<>();
    List<UUID> noTemplateUserIds = new ArrayList<>();

    for (MemberResolvedAvailability member : memberAvailability) {
      if (member.windows() == null) {
        noTemplateUserIds.add(member.userId());
        continue;
      }

      if (hasBusyOverlap(member.windows(), slotStart, slotEnd)) {
        busyUserIds.add(member.userId());
      } else {
        availableUserIds.add(member.userId());
      }
    }

    return GroupAvailabilitySlotResponse.builder()
        .startsAt(slotStart)
        .endsAt(slotEnd)
        .totalMemberCount(memberAvailability.size())
        .availableCount(availableUserIds.size())
        .busyCount(busyUserIds.size())
        .noTemplateCount(noTemplateUserIds.size())
        .availableUserIds(availableUserIds)
        .busyUserIds(busyUserIds)
        .noTemplateUserIds(noTemplateUserIds)
        .build();
  }

  private boolean hasBusyOverlap(
      List<ResolvedAvailabilityWindowResponse> windows,
      OffsetDateTime slotStart,
      OffsetDateTime slotEnd) {
    return windows.stream()
        .anyMatch(
            window ->
                window.getStatus() == AvailabilityBlockStatus.BUSY
                    && overlaps(window.getStartsAt(), window.getEndsAt(), slotStart, slotEnd));
  }

  private boolean overlaps(
      OffsetDateTime firstStart,
      OffsetDateTime firstEnd,
      OffsetDateTime secondStart,
      OffsetDateTime secondEnd) {
    return firstStart.isBefore(secondEnd) && firstEnd.isAfter(secondStart);
  }

  private void validateRange(OffsetDateTime from, OffsetDateTime to) {
    if (from == null || to == null) {
      throw new IllegalArgumentException("Availability grid requires from and to");
    }
    if (!to.isAfter(from)) {
      throw new IllegalArgumentException("Range end must be after range start");
    }
    if (Duration.between(from, to).compareTo(SLOT_DURATION) < 0) {
      throw new IllegalArgumentException("Range must be at least 15 minutes");
    }
    if (Duration.between(from, to).toDays() > MAX_GRID_DAYS) {
      throw new IllegalArgumentException(
          "Availability grid range must not be longer than " + MAX_GRID_DAYS + " days");
    }
  }

  private record MemberResolvedAvailability(
      UUID userId, List<ResolvedAvailabilityWindowResponse> windows) {}
}
