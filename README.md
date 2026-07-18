# feature-service

Spring Boot (Kotlin) service that tells the client which product modules to assemble after login.

## `GET /me`

Header: `Authorization: Bearer <Zitadel access_token>`

Returns `userInfo` (id, name, email, roles) plus the enabled `features` for those roles.

Example (dispatcher):

```json
{
  "userInfo": {
    "id": "…",
    "givenName": "Ivan",
    "familyName": "Petrov",
    "email": "ivan@example.com",
    "roles": ["dispatcher"]
  },
  "features": ["board"]
}
```

### Role → features (MVP)

| Role | Features |
|------|----------|
| `dispatcher` | `board` |
| `engineer` | `copilot` |
| other | _(none)_ |

Identity and roles come from the Zitadel JWT (claims). This service does **not** store passwords or replace the IdP.

## What this service is not

`board` and `copilot` are **feature flags** for client DI only.

- Board REST → future **dashboard-service** (`/work-orders`, …)
- Copilot REST → future **ai-gateway** (`/ai/mentor`, …)

## Local run

```bash
./gradlew bootRun
```

Env (production / resource-server):

- `ZITADEL_ISSUER` — OIDC issuer
- `ZITADEL_JWK_SET_URI` — JWKS URL

Health (no auth): `GET /actuator/health`

## Deploy

Docker Compose on the same VPS as Zitadel. Push to `master` runs tests and deploys to `/opt/feature-service`.
