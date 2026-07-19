package com.skoryk.projects.meetler.availability.api;

import com.skoryk.projects.meetler.availability.dto.AddAvailabilityBlockRequest;
import com.skoryk.projects.meetler.availability.dto.AddRecurringAvailabilityBlockRequest;
import com.skoryk.projects.meetler.availability.dto.AddSourceCalendarRequest;
import com.skoryk.projects.meetler.availability.dto.AvailabilityBlockResponse;
import com.skoryk.projects.meetler.availability.dto.AvailabilityTemplateResponse;
import com.skoryk.projects.meetler.availability.dto.ConvertAvailabilityBlockToRecurringRequest;
import com.skoryk.projects.meetler.availability.dto.CreateAvailabilityTemplateRequest;
import com.skoryk.projects.meetler.availability.dto.RecurringAvailabilityBlockResponse;
import com.skoryk.projects.meetler.availability.dto.ResolvedAvailabilityWindowResponse;
import com.skoryk.projects.meetler.availability.dto.SourceCalendarResponse;
import com.skoryk.projects.meetler.availability.dto.UpdateAvailabilityBlockRequest;
import com.skoryk.projects.meetler.availability.dto.UpdateAvailabilityTemplateRequest;
import com.skoryk.projects.meetler.availability.service.AvailabilityTemplateService;
import com.skoryk.projects.meetler.user.AppUser;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AvailabilityTemplateController implements AvailabilityTemplateApi {

  private final AvailabilityTemplateService availabilityTemplateService;

  @Override
  public ResponseEntity<AvailabilityTemplateResponse> createTemplate(
      CreateAvailabilityTemplateRequest request, AppUser user) {
    return ResponseEntity.ok(availabilityTemplateService.createTemplate(user, request));
  }

  @Override
  public ResponseEntity<Page<AvailabilityTemplateResponse>> getTemplates(AppUser user, int page) {
    return ResponseEntity.ok(availabilityTemplateService.getUserTemplates(user, page));
  }

  @Override
  public ResponseEntity<AvailabilityTemplateResponse> getTemplate(UUID templateId, AppUser user) {
    return ResponseEntity.ok(availabilityTemplateService.getTemplate(templateId, user));
  }

  @Override
  public ResponseEntity<List<ResolvedAvailabilityWindowResponse>> resolveTemplateAvailability(
      UUID templateId, OffsetDateTime from, OffsetDateTime to, AppUser user) {
    return ResponseEntity.ok(
        availabilityTemplateService.resolveTemplateAvailability(templateId, user, from, to));
  }

  @Override
  public ResponseEntity<AvailabilityTemplateResponse> updateTemplate(
      UUID templateId, UpdateAvailabilityTemplateRequest request, AppUser user) {
    return ResponseEntity.ok(availabilityTemplateService.updateTemplate(templateId, user, request));
  }

  @Override
  public ResponseEntity<Void> deleteTemplate(UUID templateId, AppUser user) {
    availabilityTemplateService.deleteTemplate(templateId, user);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<AvailabilityBlockResponse> addBlock(
      UUID templateId, AddAvailabilityBlockRequest request, AppUser user) {
    return ResponseEntity.ok(availabilityTemplateService.addBlock(templateId, user, request));
  }

  @Override
  public ResponseEntity<Page<AvailabilityBlockResponse>> getBlocks(
      UUID templateId, OffsetDateTime from, OffsetDateTime to, int page, AppUser user) {
    return ResponseEntity.ok(
        availabilityTemplateService.getBlocks(templateId, user, from, to, page));
  }

  @Override
  public ResponseEntity<AvailabilityBlockResponse> updateBlock(
      UUID templateId, UUID blockId, UpdateAvailabilityBlockRequest request, AppUser user) {
    return ResponseEntity.ok(
        availabilityTemplateService.updateBlock(templateId, blockId, user, request));
  }

  @Override
  public ResponseEntity<Void> deleteBlock(UUID templateId, UUID blockId, AppUser user) {
    availabilityTemplateService.deleteBlock(templateId, blockId, user);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<RecurringAvailabilityBlockResponse> convertBlockToRecurring(
      UUID templateId,
      UUID blockId,
      ConvertAvailabilityBlockToRecurringRequest request,
      AppUser user) {
    return ResponseEntity.ok(
        availabilityTemplateService.convertBlockToRecurring(templateId, blockId, user, request));
  }

  @Override
  public ResponseEntity<RecurringAvailabilityBlockResponse> addRecurringBlock(
      UUID templateId, AddRecurringAvailabilityBlockRequest request, AppUser user) {
    return ResponseEntity.ok(
        availabilityTemplateService.addRecurringBlock(templateId, user, request));
  }

  @Override
  public ResponseEntity<RecurringAvailabilityBlockResponse> updateRecurringBlock(
      UUID templateId, UUID blockId, AddRecurringAvailabilityBlockRequest request, AppUser user) {
    return ResponseEntity.ok(
        availabilityTemplateService.updateRecurringBlock(templateId, blockId, user, request));
  }

  @Override
  public ResponseEntity<Page<RecurringAvailabilityBlockResponse>> getRecurringBlocks(
      UUID templateId, LocalDate from, LocalDate to, int page, AppUser user) {
    return ResponseEntity.ok(
        availabilityTemplateService.getRecurringBlocks(templateId, user, from, to, page));
  }

  @Override
  public ResponseEntity<Void> deleteRecurringBlock(UUID templateId, UUID blockId, AppUser user) {
    availabilityTemplateService.deleteRecurringBlock(templateId, blockId, user);
    return ResponseEntity.noContent().build();
  }

  @Override
  public ResponseEntity<SourceCalendarResponse> addSourceCalendar(
      UUID templateId, AddSourceCalendarRequest request, AppUser user) {
    return ResponseEntity.ok(
        availabilityTemplateService.addSourceCalendar(templateId, user, request));
  }

  @Override
  public ResponseEntity<List<SourceCalendarResponse>> getSourceCalendars(
      UUID templateId, AppUser user) {
    return ResponseEntity.ok(availabilityTemplateService.getSourceCalendars(templateId, user));
  }

  @Override
  public ResponseEntity<Void> deleteSourceCalendar(UUID templateId, UUID sourceId, AppUser user) {
    availabilityTemplateService.deleteSourceCalendar(templateId, sourceId, user);
    return ResponseEntity.noContent().build();
  }
}
