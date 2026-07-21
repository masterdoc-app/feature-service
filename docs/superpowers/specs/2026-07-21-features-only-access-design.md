# Features-only access — design

Date: 2026-07-21  
Status: approved (conversation)  
Repos: feature-service, api-gateway-service, client-app, masterdoc-zitadel  
Supersedes (product language): role→feature mapping and `roles` on `GET /me` / admin invite payloads from earlier MVP docs.

## Goal

Product access is expressed only as **features** (wire strings).  
An admin invites a user by email and chooses which **features** that user gets.  
The client never branches on IdP “roles”; it only consumes `features` from `GET /me` and sends `features` on admin APIs.

## Non-goals

- Keeping legacy product keys (`admin`, `dispatcher`, `engineer`, …) in our OpenAPI / README / client DTOs
- Custom password-reset REST (Zitadel Login UI)
- Self-signup
- Creating client Organizations
- One-shot migration of every existing production grant (separate ops script if needed)
- Adding wire features that are not in the MVP catalog below (`tickets`, `map`, …) unless explicitly extended later

## Domain model

| Concept | Definition |
|---------|------------|
| Feature | Stable wire string used in JWT grants, `/me`, admin APIs, and client nav |
| Feature set | Non-empty list of features assigned to a user (invite / update) |
| Profile | Client-local capability: always added in `ClientSession.fromMe`; **not** stored in IdP |

### MVP feature catalog

| Wire value | Meaning (client) |
|------------|------------------|
| `board` | Board / work board |
| `copilot` | Copilot |
| `charts` | Charts |
| `equipment` | Equipment |
| `user_invite` | Users admin (list + invite) |

IdP note: Zitadel Management API still uses the field name `roleKey`. In **our** docs, OpenAPI, and client code we call these **feature keys**. Values of those keys = wire features above.

## Architecture

```
Client
  → GET  /me                      (profile + features)
  → GET  /admin/users             (list; requires feature user_invite)
  → POST /admin/users/invites     (email + feature set)
  → PUT  /admin/users/{id}/features  (replace feature set)
       ↓
api-gateway
  → /me → feature-service
  → /admin/users* → Zitadel Management API
       (project grants use feature wire strings as roleKey)
feature-service
  → JWT project grants → filter to known catalog → MeResponse
masterdoc-zitadel
  → terraform / expected.yaml define feature keys (not legacy role names)
```

## Contracts

### `GET /me` (feature-service → gateway proxy)

**Response:**

```json
{
  "userInfo": {
    "id": "…",
    "givenName": "Ivan",
    "familyName": "Petrov",
    "email": "ivan@example.com"
  },
  "features": ["board", "user_invite"]
}
```

- `userInfo` has **no** `roles` field.
- `features` is the **sorted** list of known wire values from the JWT project-grant claim.
- Unknown grant keys are ignored (not echoed).

Internal implementation: extract grant keys from JWT → intersect with catalog → `MeResponse`. No role→feature mapping table.

### Admin APIs (api-gateway)

Rename product language from roles → features. Behavior stays: Bearer JWT + caller must have `user_invite`.

#### `POST /admin/users/invites`

```json
{
  "email": "user@company.ru",
  "givenName": "Ivan",
  "familyName": "Petrov",
  "features": ["charts", "equipment"]
}
```

- `features`: required, non-empty, each value ∈ MVP catalog.
- Creates Zitadel human (invite email) + project grant with those keys.

#### `GET /admin/users`

List users; each item exposes `features` (not `roles`).

#### `PUT /admin/users/{id}/features`

Replaces the user’s project grant keys with the given feature set.

```json
{ "features": ["board", "copilot"] }
```

(Endpoint path rename from `/roles` → `/features`. Old path not kept.)

#### `POST /admin/users/{id}/resend-invite`

Unchanged semantically.

### Gateway helpers

- `ProductRoles` → `ProductFeatures` with `ALL` = MVP catalog; validate messages say “feature”, not “role”.
- OpenAPI: `UserInfo` without `roles`; admin schemas use `features`; docs text updated.

## Zitadel (masterdoc-zitadel)

- Terraform `local.roles` / expected list: replace legacy keys with MVP feature catalog keys.
- Display names can be human-readable (`Board`, `User invite`, …).
- Scripts (`ensure-*-platform.sh`, demo org): grant/create feature keys; README/RUNBOOK say “feature keys” when describing product access.
- Existing environments: ops may need to recreate grants under new keys (out of band).

## client-app

### Auth / session

- `UserInfoDto`: drop `roles`.
- `ClientSession.fromMe`: map `me.features` via `FeatureId.fromWire`; always add `Profile`.
- Logs: print features, not roles.
- `RoleFeatureFixtures`: rename/repurpose to feature-set fixtures for previews/tests only (no production path).

### Users screen (`FeatureId.Users` / `user_invite`)

Replace stub with:

1. **List** — `GET /admin/users` (name, email, features, state; optional resend).
2. **Invite** — form: email, givenName, familyName, multi-select of grantable features (MVP catalog except not requiring `profile`) → `POST /admin/users/invites`.
3. Errors: surface 400/403/409/502 clearly.

Admin decides the feature set freely (any non-empty subset of the catalog).

### TDD

Implement with test-first (RED → GREEN → REFACTOR):

- feature-service: `/me` JSON has no `roles`; features passthrough from JWT grant keys.
- gateway: invite/list/set-features payloads and OpenAPI-aligned DTOs.
- client: DTO decode without `roles`; invite request encoding; session mapping; Users screen logic tests where practical.

## Documentation sweep

Replace “role / roles” with “feature / features / feature set” in product docs for:

- feature-service README
- api-gateway OpenAPI + README (admin + `/me`)
- client-app comments/README where they describe access
- masterdoc-zitadel RUNBOOK / script comments (product sense; IdP API names may remain)

Do **not** rewrite Zitadel upstream API field names in code that talks to Management API (`roleKeys` stays as Zitadel’s wire).

## Error handling

| Case | Behavior |
|------|----------|
| Empty `features` on invite/update | `400` |
| Unknown feature string | `400` |
| Missing `user_invite` on caller | `403` |
| Duplicate email | `409` |
| Zitadel down | `502` |
| Invalid JWT on `/me` or admin | `401` |

## Testing plan (acceptance)

- [ ] Dispatcher-equivalent user: JWT grants `board` → `/me` returns `features:["board"]`, no `userInfo.roles`.
- [ ] Admin-equivalent: grant `user_invite` → can open Users and invite with e.g. `["charts","equipment"]`.
- [ ] Invitee’s next `/me` returns exactly the assigned known features.
- [ ] Client Users UI: list + invite by email with feature checkboxes; no role terminology in UI copy.
- [ ] Gateway OpenAPI and feature-service README mention features only for product access.

## Out of scope / follow-ups

- Production grant migration script for old keys → new feature keys
- Expanding catalog (`tickets`, `map`, …)
- Editing features from list row UI beyond replace-endpoint wiring (MVP: invite + list; PUT may ship with list later in same plan if cheap)
