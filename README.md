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
| `black_box` | Чёрный ящик |
| `board` | Доска |
| `charts` | ППР |
| `equipment` | Оборудование |

`GET /features` (Bearer) returns `{ "items": [ { "id", "titleRu" }, ... ] }` — source of truth for invite UI.

`GET /users/{userId}/features` (internal) returns `{ "features": [...] }` for that **target** user via Zitadel Management grants (requires `ZITADEL_MGMT_TOKEN` + `ZITADEL_PROJECT_ID` + header `X-Org-Id`). Not proxied by the public gateway; Compose binds the port to `127.0.0.1` only. Used by dashboard-service for WO assignee eligibility (`equipment`).

Grant keys in the Zitadel JWT that are not in this catalog are ignored. This service does **not** store passwords or replace the IdP.

## What this service is not

`board` is a **feature flag** for client DI / product ACL (dispatcher board). Engineer work-order access is `equipment`. The former `copilot` / «Наставник» grant is removed — WO assistant is core to engineer work, not a grantable feature.

- Board REST → **dashboard-service** (`/work-orders`, …)
- Equipment / documents / AI agents → catalog + technologist routes via api-gateway

## Local run

```bash
./gradlew bootRun
```

Env (production / resource-server):

- `ZITADEL_ISSUER` — OIDC issuer
- `ZITADEL_JWK_SET_URI` — JWKS URL
- `ZITADEL_MGMT_TOKEN` / `ZITADEL_PROJECT_ID` — optional; enable `GET /users/{id}/features`

Health (no auth): `GET /actuator/health`

## Deploy

Docker Compose on the same VPS as Zitadel. Push to `master` runs tests and deploys to `/opt/feature-service`.
