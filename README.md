# feature-service

Spring Boot (Kotlin) service that tells the client which product modules to assemble after login.

## `GET /me`

Header: `Authorization: Bearer <Zitadel access_token>`

Returns `userInfo` (id, name, email) plus the enabled `features` from the JWT project grants.

Example (board + admin):

```json
{
  "userInfo": {
    "id": "…",
    "givenName": "Ivan",
    "familyName": "Petrov",
    "email": "ivan@example.com"
  },
  "features": ["board", "admin"]
}
```

### MVP feature catalog

| Wire value | `titleRu` |
|------------|-----------|
| `admin` | Админ |
| `board` | Доска |
| `copilot` | Наставник |
| `charts` | ППР |
| `equipment` | Оборудование |

`GET /features` (Bearer) returns `{ "items": [ { "id", "titleRu" }, ... ] }` — source of truth for invite UI.

Grant keys in the Zitadel JWT that are not in this catalog are ignored. This service does **not** store passwords or replace the IdP.

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
