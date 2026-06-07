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
import com.skoryk.projects.meetler.user.AppUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Tag(name = "Availability Templates")
@RequestMapping("/api/availability/templates")
public interface AvailabilityTemplateApi {

  @Operation(
      summary = "Create an availability template",
      description =
          "Creates a reusable availability template for the authenticated user. "
              + "If isDefault is true, the user's previous default template is cleared.")
  @PostMapping
  ResponseEntity<AvailabilityTemplateResponse> createTemplate(
      @Valid @RequestBody CreateAvailabilityTemplateRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "List availability templates",
      description =
          "Returns availability templates owned by the authenticated user, paged in batches of 50.")
  @GetMapping
  ResponseEntity<Page<AvailabilityTemplateResponse>> getTemplates(
      @AuthenticationPrincipal AppUser user, @RequestParam(defaultValue = "0") int page);

  @Operation(
      summary = "Get an availability template",
      description = "Returns one availability template owned by the authenticated user.")
  @GetMapping("/{templateId}")
  ResponseEntity<AvailabilityTemplateResponse> getTemplate(
      @PathVariable UUID templateId, @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Resolve availability template",
      description =
          "Expands one availability template into concrete availability windows for a date-time range. "
              + "Recurring rules are expanded in the template timezone, and one-off blocks override overlapping recurring windows. "
              + "The requested range must not be longer than 366 days.")
  @GetMapping("/{templateId}/resolved")
  ResponseEntity<List<ResolvedAvailabilityWindowResponse>> resolveTemplateAvailability(
      @PathVariable UUID templateId,
      @RequestParam OffsetDateTime from,
      @RequestParam OffsetDateTime to,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Update an availability template",
      description =
          "Updates the template name, timezone, and default flag. "
              + "If isDefault is true, the user's previous default template is cleared.")
  @PatchMapping("/{templateId}")
  ResponseEntity<AvailabilityTemplateResponse> updateTemplate(
      @PathVariable UUID templateId,
      @Valid @RequestBody UpdateAvailabilityTemplateRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Delete an availability template",
      description =
          "Deletes an availability template and its related blocks and source calendar links.")
  @DeleteMapping("/{templateId}")
  ResponseEntity<Void> deleteTemplate(
      @PathVariable UUID templateId, @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Create a one-off availability block",
      description =
          "Adds a timestamped availability override to the template, such as a single busy evening "
              + "or a manually available time window.")
  @PostMapping("/{templateId}/blocks")
  ResponseEntity<AvailabilityBlockResponse> addBlock(
      @PathVariable UUID templateId,
      @Valid @RequestBody AddAvailabilityBlockRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "List one-off availability blocks",
      description =
          "Returns timestamped availability overrides for the selected template, paged in batches of 50. "
              + "Use from and to to return blocks overlapping a date-time range.")
  @GetMapping("/{templateId}/blocks")
  ResponseEntity<Page<AvailabilityBlockResponse>> getBlocks(
      @PathVariable UUID templateId,
      @RequestParam(required = false) OffsetDateTime from,
      @RequestParam(required = false) OffsetDateTime to,
      @RequestParam(defaultValue = "0") int page,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Update a one-off availability block",
      description =
          "Updates the start, end, status, and note of a timestamped availability override.")
  @PatchMapping("/{templateId}/blocks/{blockId}")
  ResponseEntity<AvailabilityBlockResponse> updateBlock(
      @PathVariable UUID templateId,
      @PathVariable UUID blockId,
      @Valid @RequestBody UpdateAvailabilityBlockRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Delete a one-off availability block",
      description = "Deletes a timestamped availability override from the selected template.")
  @DeleteMapping("/{templateId}/blocks/{blockId}")
  ResponseEntity<Void> deleteBlock(
      @PathVariable UUID templateId,
      @PathVariable UUID blockId,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Convert a one-off block to a recurring availability block",
      description =
          "Creates a recurring rule from an existing timestamped availability block. "
              + "The original block supplies start time, end time, status, and note. "
              + "If dayOfWeek, dayOfMonth, monthOfYear, or startsOn are omitted, they are inferred from the original block date. "
              + "Set deleteOriginalBlock to false to keep the original one-off block.")
  @PostMapping("/{templateId}/blocks/{blockId}/convert-to-recurring")
  ResponseEntity<RecurringAvailabilityBlockResponse> convertBlockToRecurring(
      @PathVariable UUID templateId,
      @PathVariable UUID blockId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = "application/json",
                      schema =
                          @Schema(
                              implementation = ConvertAvailabilityBlockToRecurringRequest.class),
                      examples = {
                        @ExampleObject(
                            name = "Convert to weekly",
                            summary = "Repeat the original block weekly",
                            value =
                                """
                                {
                                  "frequency": "WEEKLY",
                                  "intervalCount": 1,
                                  "occurrenceCount": null,
                                  "endsOn": null,
                                  "deleteOriginalBlock": true
                                }
                                """),
                        @ExampleObject(
                            name = "Convert to exactly two occurrences",
                            summary = "Repeat once after the original occurrence",
                            value =
                                """
                                {
                                  "frequency": "WEEKLY",
                                  "intervalCount": 1,
                                  "occurrenceCount": 2,
                                  "deleteOriginalBlock": true
                                }
                                """)
                      }))
          @Valid
          @RequestBody
          ConvertAvailabilityBlockToRecurringRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Create a recurring availability block",
      description =
          "Creates a daily, weekly, monthly, or yearly recurring availability rule. "
              + "Omit endsOn and occurrenceCount, or send them as null, to repeat without an end condition. "
              + "intervalCount controls spacing, while occurrenceCount controls how many times the rule occurs.")
  @PostMapping("/{templateId}/recurring-blocks")
  ResponseEntity<RecurringAvailabilityBlockResponse> addRecurringBlock(
      @PathVariable UUID templateId,
      @io.swagger.v3.oas.annotations.parameters.RequestBody(
              required = true,
              content =
                  @Content(
                      mediaType = "application/json",
                      schema = @Schema(implementation = AddRecurringAvailabilityBlockRequest.class),
                      examples = {
                        @ExampleObject(
                            name = "Daily",
                            summary = "Every day",
                            value =
                                """
                                {
                                  "frequency": "DAILY",
                                  "intervalCount": 1,
                                  "occurrenceCount": 2,
                                  "startTime": "09:00:00",
                                  "endTime": "17:00:00",
                                  "status": "AVAILABLE",
                                  "startsOn": "2026-06-06",
                                  "endsOn": null
                                }
                                """),
                        @ExampleObject(
                            name = "Weekly",
                            summary = "Every Monday",
                            value =
                                """
                                {
                                  "frequency": "WEEKLY",
                                  "intervalCount": 1,
                                  "occurrenceCount": null,
                                  "dayOfWeek": "MONDAY",
                                  "startTime": "09:00:00",
                                  "endTime": "17:00:00",
                                  "status": "AVAILABLE"
                                }
                                """),
                        @ExampleObject(
                            name = "Monthly",
                            summary = "Every 15th day of the month",
                            value =
                                """
                                {
                                  "frequency": "MONTHLY",
                                  "intervalCount": 1,
                                  "dayOfMonth": 15,
                                  "startTime": "18:00:00",
                                  "endTime": "22:00:00",
                                  "status": "BUSY"
                                }
                                """),
                        @ExampleObject(
                            name = "Yearly",
                            summary = "Every December 24",
                            value =
                                """
                                {
                                  "frequency": "YEARLY",
                                  "intervalCount": 1,
                                  "monthOfYear": 12,
                                  "dayOfMonth": 24,
                                  "startTime": "00:00:00",
                                  "endTime": "23:59:00",
                                  "status": "BUSY"
                                }
                                """)
                      }))
          @Valid
          @RequestBody
          AddRecurringAvailabilityBlockRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "List recurring availability blocks",
      description =
          "Returns daily, weekly, monthly, and yearly recurring availability rules for the template, paged in batches of 50. "
              + "Use from and to to return rules active during a date range.")
  @GetMapping("/{templateId}/recurring-blocks")
  ResponseEntity<Page<RecurringAvailabilityBlockResponse>> getRecurringBlocks(
      @PathVariable UUID templateId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(defaultValue = "0") int page,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Delete a recurring availability block",
      description = "Deletes a recurring availability rule from the selected template.")
  @DeleteMapping("/{templateId}/recurring-blocks/{blockId}")
  ResponseEntity<Void> deleteRecurringBlock(
      @PathVariable UUID templateId,
      @PathVariable UUID blockId,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Attach a source calendar",
      description =
          "Links one of the authenticated user's calendars to this template so busy events can be "
              + "used when calculating availability.")
  @PostMapping("/{templateId}/source-calendars")
  ResponseEntity<SourceCalendarResponse> addSourceCalendar(
      @PathVariable UUID templateId,
      @Valid @RequestBody AddSourceCalendarRequest request,
      @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "List source calendars",
      description = "Returns calendars linked to this template as availability sources.")
  @GetMapping("/{templateId}/source-calendars")
  ResponseEntity<List<SourceCalendarResponse>> getSourceCalendars(
      @PathVariable UUID templateId, @AuthenticationPrincipal AppUser user);

  @Operation(
      summary = "Detach a source calendar",
      description = "Removes a source calendar link from the selected availability template.")
  @DeleteMapping("/{templateId}/source-calendars/{sourceId}")
  ResponseEntity<Void> deleteSourceCalendar(
      @PathVariable UUID templateId,
      @PathVariable UUID sourceId,
      @AuthenticationPrincipal AppUser user);
}


