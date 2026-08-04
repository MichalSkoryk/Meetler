# Meetler Mobile Client README

This document is for phone app developers integrating with the Meetler backend.

The backend is a Spring Boot API. Local default base URL:

```text
http://localhost:8080
```

For Android emulator use:

```text
http://10.0.2.2:8080
```

For a physical phone, use the computer's LAN IP address, for example:

```text
http://192.168.1.20:8080
```

## Local Startup

From the `Meetler` directory:

```bash
docker compose up -d --build postgres meetler
```

Optional browser test site:

```bash
docker compose up -d --build test-site
```

Open:

```text
http://localhost:8081
```

API docs:

```text
http://localhost:8080/docs
http://localhost:8080/api-docs
```

Google and Microsoft keys are not committed. Ask the backend developer for the `.env` values.

## Auth Basics

Most endpoints require:

```http
Authorization: Bearer <accessToken>
Content-Type: application/json
```

The auth response shape is:

```json
{
  "accessToken": "jwt-access-token",
  "refreshToken": "refresh-token"
}
```

Store tokens in secure storage:

- Android: EncryptedSharedPreferences or Keystore-backed storage.
- iOS: Keychain.
- React Native/Flutter: use a secure storage plugin, not plain local storage.

## Recommended App Startup Flow

1. If no refresh token exists, show login.
2. If an access token exists, call `GET /api/me/bootstrap`.
3. If that returns `401`, call `POST /api/auth/refresh`.
4. Retry `GET /api/me/bootstrap`.
5. If refresh fails, clear tokens and show login.

Bootstrap endpoint:

```http
GET /api/me/bootstrap
Authorization: Bearer <accessToken>
```

Example response:

```json
{
  "user": {
    "id": "0a1e9df9-7d33-49b6-8e6a-3f32c9de312a",
    "email": "user@example.com",
    "name": "User",
    "role": "USER",
    "hasPassword": true
  },
  "subscription": {
    "planCode": "FREE",
    "planName": "Free",
    "ownedGroupsUsed": 1,
    "maxOwnedGroups": 2,
    "availabilityTemplatesUsed": 1,
    "maxAvailabilityTemplates": 3
  },
  "connectedCalendars": {
    "google": true,
    "microsoft": false
  }
}
```

Subscription usage only:

```http
GET /api/me/subscription/usage
Authorization: Bearer <accessToken>
```

## Email And Password Auth

Register:

```http
POST /api/auth/register
```

```json
{
  "email": "user@example.com",
  "name": "User",
  "password": "secret123"
}
```

Login:

```http
POST /api/auth/login
```

```json
{
  "email": "user@example.com",
  "password": "secret123"
}
```

Refresh:

```http
POST /api/auth/refresh
```

```json
{
  "refreshToken": "refresh-token"
}
```

Logout:

```http
POST /api/auth/logout
```

```json
{
  "refreshToken": "refresh-token"
}
```

Password reset:

```http
POST /api/auth/password/reset/request
POST /api/auth/password/reset/confirm
```

## Google And Microsoft Login

Mobile apps should use the backend OAuth login endpoints with a validated `returnUrl` and `responseMode=mobile_code`.

Example:

```text
GET /api/auth/google/login?returnUrl=https://meetler-frontend-dev.onrender.com/mobile/auth/callback&responseMode=mobile_code
GET /api/auth/microsoft/login?returnUrl=https://meetler-frontend-dev.onrender.com/mobile/auth/callback&responseMode=mobile_code
```

The backend redirects to the provider. After provider login, the backend redirects to:

```text
https://meetler-frontend-dev.onrender.com/mobile/auth/callback?code=<single-use-code>
```

Exchange that two-minute code exactly once from the app:

```http
POST /api/auth/mobile/exchange
Content-Type: application/json
```

```json
{
  "code": "single-use-code"
}
```

The exchange response contains the normal access and refresh tokens. Tokens are never placed in the OAuth return URL. HTTPS return origins must be present in `OAUTH_ALLOWED_RETURN_ORIGINS`; `meetler://` is supported as a local-development fallback.

## Guest Login

Request a guest magic link:

```http
POST /api/auth/guest/request-login
```

```json
{
  "email": "guest@example.com",
  "name": "Guest",
  "returnUrl": "https://meetler-frontend-dev.onrender.com/mobile/auth/guest"
}
```

The app should open the link from email. The backend endpoint is:

```text
GET /api/auth/guest/login?token=<token>
```

It returns the normal auth response with `accessToken` and `refreshToken`.

Password-reset requests accept the same optional `returnUrl` field. Omitting it preserves the existing web email behavior.

## Android Push Notifications

Register the native FCM token after sign-in:

```http
POST /api/me/devices
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "platform": "ANDROID",
  "provider": "FCM",
  "token": "native-fcm-token"
}
```

Persist the returned device ID and revoke it during logout:

```http
DELETE /api/me/devices/{deviceId}
Authorization: Bearer <accessToken>
```

Push data may contain `groupId` and `eventId`; route to the event when both exist, then to the group, and finally to the notification inbox.

## Availability Templates

Create template:

```http
POST /api/availability/templates
Authorization: Bearer <accessToken>
```

```json
{
  "name": "Default availability",
  "isDefault": true,
  "timezone": "Europe/Warsaw",
  "defaultAvailabilityStatus": "BUSY"
}
```

Important statuses:

- `AVAILABLE`
- `BUSY`

The app currently treats empty time as busy by default when `defaultAvailabilityStatus` is `BUSY`.

List templates:

```http
GET /api/availability/templates?page=0
```

Get template:

```http
GET /api/availability/templates/{templateId}
```

Add one-time block:

```http
POST /api/availability/templates/{templateId}/blocks
```

```json
{
  "startsAt": "2026-06-15T09:00:00+02:00",
  "endsAt": "2026-06-15T10:00:00+02:00",
  "status": "AVAILABLE",
  "source": "MANUAL",
  "note": "Morning slot"
}
```

List blocks with date filtering:

```http
GET /api/availability/templates/{templateId}/blocks?start=2026-06-15T00:00:00+02:00&end=2026-06-22T00:00:00+02:00&page=0
```

Add recurring block:

```http
POST /api/availability/templates/{templateId}/recurring-blocks
```

Weekly example:

```json
{
  "frequency": "WEEKLY",
  "intervalCount": 1,
  "dayOfWeek": "MONDAY",
  "startTime": "18:00:00",
  "endTime": "20:00:00",
  "status": "AVAILABLE",
  "startsOn": "2026-06-15"
}
```

Use `occurrenceCount` to limit how many times the rule repeats. Leave both `occurrenceCount` and `endsOn` empty for an open-ended recurring rule.

Resolved availability:

```http
GET /api/availability/templates/{templateId}/resolved?start=2026-06-15T00:00:00+02:00&end=2026-06-22T00:00:00+02:00
```

## External Calendar Import

Connect Google Calendar:

```text
GET /api/external-calendars/google/connect?returnUrl=meetler://calendar-connected
```

This endpoint requires the user to already be logged in, so open it with the auth token available in your web/auth session strategy. For a pure mobile OAuth flow, coordinate with backend if you need token handoff to the browser.

Import future Google Calendar events:

```http
POST /api/external-calendars/google/import
Authorization: Bearer <accessToken>
```

Imported external calendars can be attached as availability template sources:

```http
POST /api/availability/templates/{templateId}/source-calendars
```

```json
{
  "calendarId": "d6109fd5-cc6d-4bf5-8b04-5c63e8ea0b50",
  "includeBusyEvents": true
}
```

## Groups

Create group:

```http
POST /api/groups
```

```json
{
  "name": "Friday board games",
  "eventRequiresConfirmation": true
}
```

List my groups:

```http
GET /api/groups
```

Update group:

```http
PATCH /api/groups/{groupId}
```

```json
{
  "name": "Friday board games",
  "eventRequiresConfirmation": false
}
```

Create invite:

```http
POST /api/invites/{groupId}
```

Join by invite code:

```http
POST /api/invites/join/{code}
```

Select my availability template for a group:

```http
POST /api/groups/{groupId}/members/me/availability-template
```

```json
{
  "availabilityTemplateId": "17f72ffd-b3c7-46f3-b647-fac5af5a7078"
}
```

Group availability grid:

```http
GET /api/groups/{groupId}/availability-grid?start=2026-06-15T09:00:00+02:00&end=2026-06-15T18:00:00+02:00
```

The grid is returned in 15-minute slots:

```json
{
  "startsAt": "2026-06-15T09:00:00+02:00",
  "endsAt": "2026-06-15T09:15:00+02:00",
  "totalMemberCount": 4,
  "availableCount": 3,
  "busyCount": 1,
  "noTemplateCount": 0,
  "availableUserIds": [],
  "busyUserIds": [],
  "noTemplateUserIds": []
}
```

Admin actions:

```http
POST /api/groups/{groupId}/admin/promote/{userId}
POST /api/groups/{groupId}/admin/demote/{userId}
DELETE /api/groups/{groupId}/admin/remove/{userId}
POST /api/groups/{groupId}/admin/transfer/{newOwnerId}
```

## Group Events

Create event:

```http
POST /api/groups/{groupId}/events
```

```json
{
  "title": "Dinner",
  "description": "Pizza place near the office",
  "startsAt": "2026-06-15T19:00:00+02:00",
  "endsAt": "2026-06-15T21:00:00+02:00"
}
```

If the group has `eventRequiresConfirmation=true`, participants need to respond before the event is considered accepted for them.

List group events:

```http
GET /api/groups/{groupId}/events
```

Update event:

```http
PATCH /api/groups/{groupId}/events/{eventId}
```

Respond to event:

```http
PATCH /api/groups/{groupId}/events/{eventId}/response
```

```json
{
  "status": "ACCEPTED"
}
```

Cancel event:

```http
DELETE /api/groups/{groupId}/events/{eventId}
```

Export event to Google Calendar:

```http
POST /api/groups/{groupId}/events/{eventId}/google/export
```

## Error Format

Errors use a consistent JSON structure:

```json
{
  "timestamp": "2026-06-14T19:35:00+02:00",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_FAILED",
  "message": "Validation failed",
  "path": "/api/auth/login",
  "fields": {
    "email": "must be a well-formed email address"
  }
}
```

Common status codes:

- `400`: bad request or validation error.
- `401`: missing, expired, or invalid access token.
- `403`: logged in but not allowed.
- `404`: resource not found.
- `409`: business conflict, including subscription limits.
- `500`: unexpected backend error.

Mobile clients should prefer `code` for branching and `message` for temporary developer-facing diagnostics. User-facing copy should usually be app-owned.

## Date And Time Rules

Use ISO-8601 strings.

For exact instants use `OffsetDateTime`:

```text
2026-06-15T09:00:00+02:00
```

For recurring rule local dates and times use:

```text
2026-06-15
18:00:00
```

Availability templates also store a timezone such as:

```text
Europe/Warsaw
```

This is needed so recurring blocks like "every Monday at 18:00" resolve correctly across daylight-saving changes.

## Mobile Implementation Notes

- Treat access tokens as short-lived. Refresh when the backend returns `401`.
- Keep refresh token rotation simple: after `POST /api/auth/refresh`, replace both stored tokens with the response values.
- Always send UTC offset in event and block timestamps.
- Use `GET /api/me/bootstrap` after app launch, login, and account/calendar changes.
- Use the OpenAPI docs for exact schema details while this README stays as the practical integration guide.
