package com.skoryk.projects.meetler.group.member;

import static org.assertj.core.api.Assertions.assertThat;

import com.skoryk.projects.meetler.availability.model.AvailabilityTemplate;
import com.skoryk.projects.meetler.group.Group;
import com.skoryk.projects.meetler.group.member.dto.GroupAvailabilityTemplateResponse;
import com.skoryk.projects.meetler.group.member.dto.GroupMemberResponse;
import com.skoryk.projects.meetler.user.AppUser;
import com.skoryk.projects.meetler.user.AppUserRole;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class GroupMemberMapperTest {

  private final GroupMemberMapper mapper = Mappers.getMapper(GroupMemberMapper.class);

  @Test
  void mapsMembershipRoleAndNullTemplate() {
    UUID groupId = UUID.randomUUID();
    UUID userId = UUID.randomUUID();
    Group group = Group.builder().id(groupId).build();
    AppUser user =
        AppUser.builder()
            .id(userId)
            .name("Member")
            .email("member@example.com")
            .role(AppUserRole.USER)
            .build();
    GroupMember member =
        GroupMember.builder().group(group).user(user).role(GroupRole.OWNER).build();

    GroupMemberResponse memberResponse = mapper.toMemberResponse(member);
    GroupAvailabilityTemplateResponse templateResponse =
        mapper.toAvailabilityTemplateResponse(member);

    assertThat(memberResponse.getRole()).isEqualTo("OWNER");
    assertThat(memberResponse.getAvailabilityTemplateId()).isNull();
    assertThat(memberResponse.getAvailabilityTemplateName()).isNull();
    assertThat(templateResponse.getGroupId()).isEqualTo(groupId);
    assertThat(templateResponse.getUserId()).isEqualTo(userId);
    assertThat(templateResponse.getAvailabilityTemplateId()).isNull();
    assertThat(templateResponse.getAvailabilityTemplateName()).isNull();
    assertThat(templateResponse.getTimezone()).isNull();
  }

  @Test
  void mapsAvailabilityTemplateDetails() {
    UUID templateId = UUID.randomUUID();
    AvailabilityTemplate template =
        AvailabilityTemplate.builder()
            .id(templateId)
            .name("Weekdays")
            .timezone("Europe/Warsaw")
            .build();
    GroupMember member =
        GroupMember.builder()
            .group(Group.builder().id(UUID.randomUUID()).build())
            .user(AppUser.builder().id(UUID.randomUUID()).build())
            .role(GroupRole.MEMBER)
            .availabilityTemplate(template)
            .build();

    GroupAvailabilityTemplateResponse response = mapper.toAvailabilityTemplateResponse(member);

    assertThat(response.getAvailabilityTemplateId()).isEqualTo(templateId);
    assertThat(response.getAvailabilityTemplateName()).isEqualTo("Weekdays");
    assertThat(response.getTimezone()).isEqualTo("Europe/Warsaw");
  }
}
