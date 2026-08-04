# Meetler

To use the application you have to:

Run ```docker-compose up -d postgres``` command in the root directory.

Then run the application with ```docker-compose up -d meetler```.

Database schema migrations are managed by Flyway from ```src/main/resources/db/migration```.

## Test site

A simple nginx-hosted test site is available for local auth and calendar-connection testing.

Run:

```bash
docker compose up -d --build meetler test-site
```

Open:

```text
http://localhost:8081
```

The test site supports email/password login, logout, Google login, Microsoft login, and Google Calendar connection testing.

## Mobile client documentation

Phone app integration notes are available in:

```text
docs/phone-developers/README.md
```

## Integration keys

Google and Microsoft OAuth client IDs/secrets are not committed to the repository.

For Google or Microsoft login/calendar integration access, contact the developer for the required `.env` values.

Mobile social login uses a two-minute, single-use exchange code. Allow hosted mobile return links with:

```text
OAUTH_ALLOWED_RETURN_ORIGINS=https://meetler-frontend-dev.onrender.com
```

Firebase push delivery is opt-in. Set `FIREBASE_PUSH_ENABLED=true` and provide the service-account JSON as base64 in `FIREBASE_SERVICE_ACCOUNT_BASE64`. When Firebase is disabled, the logging sender remains active for local development.

Enjoy!
