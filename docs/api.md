# API Reference

Base URL for local development: `http://localhost:8080`

All request/response bodies use JSON. The examples use placeholder values; never commit real access or refresh tokens.

## Authentication

The following endpoints are public:

| Method | Path | Purpose | Success |
|---|---|---|---|
| POST | `/api/v1/auth/register` | Create an account | 201 |
| POST | `/api/v1/auth/login` | Authenticate with email/password | 200 |
| POST | `/api/v1/auth/refresh` | Rotate a refresh token | 200 |
| POST | `/api/v1/auth/logout` | Revoke the supplied refresh token | 204 |

### Register

`POST /api/v1/auth/register`

Request:

```json
{
  "displayName": "Manohar",
  "email": "manohar@example.com",
  "password": "use-a-strong-password"
}
```

Validation:
- Display name is required and at most 100 characters.
- Email must be valid and at most 254 characters.
- Password must be 12–72 characters and no more than 72 UTF-8 bytes (BCrypt input limit).

A successful response contains the user profile, a 15-minute Bearer access token and a 7-day refresh token. The refresh token is opaque, randomly generated, and only its SHA-256 hash is persisted.

### Login

`POST /api/v1/auth/login`

Request:

```json
{
  "email": "manohar@example.com",
  "password": "use-a-strong-password"
}
```

Unknown emails and incorrect passwords return the same generic authentication error.

### Refresh

`POST /api/v1/auth/refresh`

Request:

```json
{
  "refreshToken": "<refresh-token-returned-by-register-or-login>"
}
```

The current refresh token is revoked and a new token pair is issued. Reusing a revoked or expired refresh token is rejected.

### Logout

`POST /api/v1/auth/logout`

Request:

```json
{
  "refreshToken": "<current-refresh-token>"
}
```

Returns `204 No Content`. Logout revokes the supplied refresh token. An already-issued access token remains valid until its short expiry (15 minutes).

## Authenticated requests

Every endpoint outside the public authentication paths and health endpoint requires:

```http
Authorization: Bearer <access-token>
```

The access token contains:
- `sub`: normalized user email
- `userId`: user UUID
- `platformRole`: `USER` or `ADMIN`
- `iss`: `ai-trip-war-room`
- `iat` and `exp`: issue and expiry timestamps

Trip-scoped roles (`VIEWER`, `PLANNER`, `LEADER`) are stored in trip memberships and will be checked in the trip/application layer. They are deliberately not treated as global user authorities.

## Health

`GET /actuator/health` is public and provides the application health response.

## Error behavior

- `400 Bad Request`: invalid request body / validation failure.
- `401 Unauthorized`: invalid credentials, invalid/revoked/expired refresh token, or missing/invalid access token on a protected endpoint.
- `409 Conflict`: attempted registration using an email that already exists.

The exact framework-generated validation/error JSON may evolve as the API error response format is standardized.
