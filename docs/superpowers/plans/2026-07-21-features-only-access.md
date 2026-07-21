# Features-only access Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Product access is only features: `/me` drops `roles`, IdP grant keys = wire features, admin invite/list use `features`, client Users screen invites by email with feature checkboxes.

**Architecture:** Zitadel project grant keys become MVP wire features (`board`, `copilot`, `charts`, `equipment`, `user_invite`). feature-service filters JWT grants to that catalog and returns `userInfo` + `features` (no roles). api-gateway admin APIs rename `roles` → `features` and `PUT …/features`. client-app drops `roles` from DTOs and implements Users list + invite against gateway.

**Tech Stack:** Kotlin, Spring Boot (feature-service), Ktor (api-gateway), KMP Compose (client-app), Zitadel Terraform (masterdoc-zitadel), JUnit5 / kotlin.test, MockMvc / Ktor testApplication.

**Spec:** `docs/superpowers/specs/2026-07-21-features-only-access-design.md`

## Global Constraints

- Product language: **feature / features / feature set** only (never “role” in our OpenAPI, README, client UI copy, or public JSON fields).
- MVP catalog (exact): `board`, `copilot`, `charts`, `equipment`, `user_invite`.
- Zitadel Management API field name `roleKeys` may remain in gateway↔IdP code; values must be catalog wire strings.
- `profile` is client-local only (always added in `ClientSession.fromMe`); never grant/store in IdP.
- TDD: failing test first for each behavior change; run **targeted** unit/integration tests only (no full Wasm/desktop distribution builds locally — CI after push).
- Repos are separate git roots under `/Users/antonbutov/Documents/MYPROJECTS/fixaverse/` — commit in the repo you change.

## File map

| Area | Files |
|------|--------|
| feature-service | `MeResponse.kt`, `JwtUserExtractor.kt`, `MeService.kt`, replace `RoleFeatureResolver.kt` → `FeatureCatalog.kt`, tests, `README.md` |
| masterdoc-zitadel | `terraform/main.tf` locals, `terraform/expected.yaml`, scripts/RUNBOOK product wording |
| api-gateway | `ProductRoles.kt` → `ProductFeatures.kt`, `AdminUserModels.kt`, `AdminUserRoutes.kt`, `ZitadelAdminClient.kt`, `ZitadelAdminDtos.kt` mapping, `openapi.yaml`, tests |
| client-app | `AuthModels.kt`, `AuthRepository.kt`, `ClientSession.kt`, new admin models/repo, `UsersScreen`, wire `MainShellContent` |

---

### Task 1: feature-service — `/me` without roles, features from JWT grants

**Files:**
- Modify: `src/main/kotlin/pro/masterdoc/feature/api/MeResponse.kt`
- Modify: `src/main/kotlin/pro/masterdoc/feature/auth/JwtUserExtractor.kt`
- Create: `src/main/kotlin/pro/masterdoc/feature/features/FeatureCatalog.kt`
- Delete: `src/main/kotlin/pro/masterdoc/feature/features/RoleFeatureResolver.kt`
- Modify: `src/main/kotlin/pro/masterdoc/feature/features/MeService.kt`
- Modify: `src/test/kotlin/pro/masterdoc/feature/api/MeControllerTest.kt`
- Modify: `src/test/kotlin/pro/masterdoc/feature/features/RoleFeatureResolverTest.kt` → rename to `FeatureCatalogTest.kt`
- Modify: `README.md`

**Interfaces:**
- Consumes: JWT claim `urn:zitadel:iam:org:project:roles` (map keys) or `roles` collection — IdP claim name unchanged
- Produces: `UserInfoDto(id, givenName, familyName, email)` **without** `roles`; `MeResponse(userInfo, features: List<String>)` sorted known features; `FeatureCatalog.filter(grantKeys: List<String>): List<String>`

- [ ] **Step 1: Write the failing test** (assert no `roles` in JSON; grant key `board` → feature `board`)

In `MeControllerTest.kt`, replace/add:

```kotlin
@Test
fun `getMe returns features from grant keys without roles field`() {
    val token = Jwt.withTokenValue("test-token")
        .header("alg", "none")
        .subject("user-1")
        .claim("given_name", "Ivan")
        .claim("family_name", "Petrov")
        .claim("email", "ivan@example.com")
        .claim(
            "urn:zitadel:iam:org:project:roles",
            mapOf("board" to mapOf("project-id" to "zitadel.localhost")),
        )
        .issuedAt(Instant.now())
        .expiresAt(Instant.now().plusSeconds(3600))
        .build()

    mockMvc.get("/me") {
        with(jwt().jwt(token))
    }.andExpect {
        status { isOk() }
        jsonPath("$.userInfo.id") { value("user-1") }
        jsonPath("$.userInfo.givenName") { value("Ivan") }
        jsonPath("$.userInfo.roles") { doesNotExist() }
        jsonPath("$.features[0]") { value("board") }
    }
}
```

In new `FeatureCatalogTest.kt`:

```kotlin
class FeatureCatalogTest {
    private val catalog = FeatureCatalog()

    @Test
    fun `keeps known grant keys sorted`() {
        assertEquals(listOf("board", "charts"), catalog.filter(listOf("charts", "board", "unknown")))
    }

    @Test
    fun `drops unknown keys`() {
        assertEquals(emptyList(), catalog.filter(listOf("admin", "dispatcher")))
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run (from `feature-service/`):

```bash
./gradlew test --tests pro.masterdoc.feature.api.MeControllerTest --tests pro.masterdoc.feature.features.FeatureCatalogTest
```

Expected: FAIL — `roles` still present and/or `FeatureCatalog` missing; old mapping still turns `dispatcher`→`board`.

- [ ] **Step 3: Minimal implementation**

`MeResponse.kt`:

```kotlin
package pro.masterdoc.feature.api

data class UserInfoDto(
    val id: String,
    val givenName: String?,
    val familyName: String?,
    val email: String?,
)

data class MeResponse(
    val userInfo: UserInfoDto,
    val features: List<String>,
)
```

Internal claims holder (same file as extractor or small data class in `auth`):

```kotlin
data class JwtUserClaims(
    val id: String,
    val givenName: String?,
    val familyName: String?,
    val email: String?,
    val grantKeys: List<String>,
)
```

`JwtUserExtractor.extract` returns `JwtUserClaims` (grant keys from existing claim parsing; rename local `roles` → `grantKeys`).

`FeatureCatalog.kt`:

```kotlin
package pro.masterdoc.feature.features

import org.springframework.stereotype.Component

@Component
class FeatureCatalog {
    companion object {
        val ALL: Set<String> = setOf("board", "copilot", "charts", "equipment", "user_invite")
    }

    fun filter(grantKeys: List<String>): List<String> =
        grantKeys.filter { it in ALL }.toSortedSet().toList()
}
```

`MeService`:

```kotlin
fun getMe(jwt: Jwt): MeResponse {
    val claims = jwtUserExtractor.extract(jwt)
    val features = featureCatalog.filter(claims.grantKeys)
    return MeResponse(
        userInfo = UserInfoDto(claims.id, claims.givenName, claims.familyName, claims.email),
        features = features,
    )
}
```

Delete `RoleFeatureResolver.kt`. Update/remove old role-mapping tests. Update `README.md` example JSON (no `roles`; catalog table = wire features).

- [ ] **Step 4: Run tests to verify they pass**

```bash
./gradlew test --tests 'pro.masterdoc.feature.*'
```

Expected: PASS

- [ ] **Step 5: Commit** (in `feature-service`)

```bash
git add src README.md
git commit -m "feat: expose only features on GET /me"
```

---

### Task 2: masterdoc-zitadel — feature keys in Terraform / expected

**Files:**
- Modify: `terraform/main.tf` (`locals.roles` map keys → feature wires; display names human-readable)
- Modify: `terraform/expected.yaml` (`roles:` list → feature keys; keep YAML key name if verify suite expects `roles` field — if so, add comment that values are feature keys, OR rename to `features` and update verify Models accordingly)
- Modify: `scripts/ensure-technologist-platform.sh`, `scripts/ensure-demo-org.sh` — create/grant catalog keys
- Modify: `docs/RUNBOOK.md` — product wording “feature keys”

**Interfaces:**
- Consumes: existing Terraform `zitadel_project_role` resources
- Produces: IdP project roleKeys = `board`, `copilot`, `charts`, `equipment`, `user_invite`

- [ ] **Step 1: Update expected catalog first (failing verify if suite runs)**

`terraform/expected.yaml`:

```yaml
roles:
  - board
  - charts
  - copilot
  - equipment
  - user_invite
```

(Keep property name `roles` only if `ExpectedPlatform` / verify loader requires it; document in comment: `# feature keys (Zitadel roleKey values)`.)

- [ ] **Step 2: Run verify unit tests if present**

```bash
cd verify && ./gradlew test --tests pro.masterdoc.zitadel.verify.ZitadelInvariantsTest
```

Expected: FAIL or mismatch until `main.tf` + loader align — or PASS if tests only check YAML parse.

- [ ] **Step 3: Update `main.tf` locals**

```hcl
locals {
  roles = {
    board       = "Board"
    copilot     = "Copilot"
    charts      = "Charts"
    equipment   = "Equipment"
    user_invite = "User invite"
  }
}
```

Update scripts: `ensure_role board Board` etc.; demo grants use feature keys (e.g. default invite `user_invite` or `board` as needed for smoke).

- [ ] **Step 4: Re-run verify unit tests**

Expected: PASS (live IT optional / CI).

- [ ] **Step 5: Commit** (in `masterdoc-zitadel`)

```bash
git add terraform scripts docs
git commit -m "feat: use feature wire keys as Zitadel project grants"
```

---

### Task 3: api-gateway — ProductFeatures + admin DTOs

**Files:**
- Create: `src/main/kotlin/pro/masterdoc/gateway/ProductFeatures.kt`
- Delete: `src/main/kotlin/pro/masterdoc/gateway/ProductRoles.kt`
- Modify: `src/main/kotlin/pro/masterdoc/gateway/AdminUserModels.kt`
- Modify: `src/test/kotlin/pro/masterdoc/gateway/ProductRolesTest.kt` → `ProductFeaturesTest.kt`
- Modify: mapping helpers that set `AdminUser.roles` → `features`

**Interfaces:**
- Consumes: feature string lists from HTTP
- Produces: `ProductFeatures.validate(features: List<String>): String?`; `InviteUserRequest.features`; `SetFeaturesRequest(features)`; `AdminUser.features`

- [ ] **Step 1: Write failing tests**

`ProductFeaturesTest.kt`:

```kotlin
class ProductFeaturesTest {
    @Test
    fun `rejects unknown feature`() {
        assertEquals("Unknown feature: foo", ProductFeatures.validate(listOf("board", "foo")))
    }

    @Test
    fun `rejects empty features`() {
        assertEquals("features must not be empty", ProductFeatures.validate(emptyList()))
    }

    @Test
    fun `accepts known features`() {
        assertEquals(null, ProductFeatures.validate(listOf("user_invite", "charts")))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

```bash
cd /Users/antonbutov/Documents/MYPROJECTS/fixaverse/api-gateway-service
./gradlew test --tests pro.masterdoc.gateway.ProductFeaturesTest
```

Expected: FAIL — class missing

- [ ] **Step 3: Implement ProductFeatures + rename DTO fields**

```kotlin
object ProductFeatures {
    val ALL: Set<String> = setOf("board", "copilot", "charts", "equipment", "user_invite")

    fun validate(features: List<String>): String? {
        if (features.isEmpty()) return "features must not be empty"
        for (f in features) {
            if (f !in ALL) return "Unknown feature: $f"
        }
        return null
    }
}
```

`AdminUserModels.kt`:

```kotlin
@Serializable
data class InviteUserRequest(
    val email: String,
    val givenName: String,
    val familyName: String,
    val features: List<String>,
)

@Serializable
data class SetFeaturesRequest(val features: List<String>)

@Serializable
data class AdminUser(
    val id: String,
    val email: String,
    val givenName: String,
    val familyName: String,
    val features: List<String>,
    val state: String,
    val inviteSent: Boolean? = null,
)
```

Update `ZitadelAdminMapping.toAdminUser(..., features: List<String>, ...)` and in-memory/HTTP client: pass `request.features` into `ZitadelCreateGrantRequest(..., roleKeys = request.features)` / update grant. Rename `setRoles` → `setFeatures` on `ZitadelAdminClient`.

- [ ] **Step 4: Run ProductFeaturesTest + compile-sensitive tests**

```bash
./gradlew test --tests pro.masterdoc.gateway.ProductFeaturesTest
```

Expected: PASS (other tests may still fail until Task 4 — fix compile errors enough to compile).

- [ ] **Step 5: Commit**

```bash
git add src
git commit -m "feat: rename admin product roles to features in gateway DTOs"
```

---

### Task 4: api-gateway — routes, OpenAPI, admin tests

**Files:**
- Modify: `src/main/kotlin/pro/masterdoc/gateway/AdminUserRoutes.kt`
- Modify: `src/test/kotlin/pro/masterdoc/gateway/AdminUserRoutesTest.kt`
- Modify: `src/test/kotlin/pro/masterdoc/gateway/MeRoutesTest.kt` (fixture JSON: drop `roles` from sample `/me` body)
- Modify: `openapi.yaml`
- Modify: `README.md` if it mentions roles on `/me` or invite

**Interfaces:**
- Consumes: `ProductFeatures`, `InviteUserRequest`, `SetFeaturesRequest`
- Produces: `POST /admin/users/invites` body `features`; `PUT /admin/users/{id}/features`; list items with `features`

- [ ] **Step 1: Update failing route tests first**

In `AdminUserRoutesTest`, replace all `"roles"` JSON keys with `"features"`, values with catalog wires (e.g. `["user_invite"]`, `["charts","equipment"]`). Change:

- `PUT /admin/users/$id/roles` → `PUT /admin/users/$id/features`
- Assertions `body["roles"]` → `body["features"]`
- Fake `/me` for authz: `"features":["user_invite"]` (no roles needed)

`MeRoutesTest` stub body:

```json
{"userInfo":{"id":"u1","givenName":"Ivan","familyName":"Petrov","email":"i@e.com"},"features":["board"]}
```

- [ ] **Step 2: Run tests to verify fail/compile align**

```bash
./gradlew test --tests pro.masterdoc.gateway.AdminUserRoutesTest --tests pro.masterdoc.gateway.MeRoutesTest
```

Expected: FAIL until routes updated (or compile errors pointing at old names).

- [ ] **Step 3: Update routes + OpenAPI**

`AdminUserRoutes.kt`: `ProductFeatures.validate(request.features)`; `put("/admin/users/{id}/features")` calling `setFeatures`.

OpenAPI: `UserInfo` remove `roles` from required/properties; `InviteUserRequest.features` enum = catalog; path `/admin/users/{id}/features`; `AdminUser.features`.

- [ ] **Step 4: Run gateway tests**

```bash
./gradlew test --tests 'pro.masterdoc.gateway.*'
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add src openapi.yaml README.md
git commit -m "feat: admin APIs assign features; /me schema drops roles"
```

---

### Task 5: client-app — drop `roles` from auth DTOs / session fixtures

**Files:**
- Modify: `auth/src/commonMain/kotlin/pro/masterdoc/client/auth/AuthModels.kt`
- Modify: `auth/src/commonMain/kotlin/pro/masterdoc/client/auth/AuthRepository.kt` (log line)
- Modify: `shared/src/commonMain/kotlin/pro/masterdoc/client/session/ClientSession.kt` (rename `RoleFeatureFixtures` → `FeatureSetFixtures` with feature-wire helpers for tests)
- Modify: `shared/src/commonTest/kotlin/pro/masterdoc/client/session/ClientSessionFromMeTest.kt`
- Modify: `shared/src/commonTest/kotlin/pro/masterdoc/client/navigation/NavMenuBuilderTest.kt` if it uses old fixture names

**Interfaces:**
- Consumes: `MeResponse` JSON without `roles`
- Produces: `UserInfoDto` without `roles`; fixtures take feature sets / wire lists

- [ ] **Step 1: Update tests to construct `UserInfoDto` without roles**

```kotlin
UserInfoDto(id = "u1", email = "a@b.com")
```

Add decode test in `AuthRepositoryTest` or small serialization test:

```kotlin
@Test
fun meResponse_decodesWithoutRoles() {
    val json = """{"userInfo":{"id":"u1","givenName":"I","familyName":"P","email":"i@e.com"},"features":["board"]}"""
    val me = Json { ignoreUnknownKeys = true }.decodeFromString(MeResponse.serializer(), json)
    assertEquals(listOf("board"), me.features)
    assertEquals("u1", me.userInfo.id)
}
```

- [ ] **Step 2: Run targeted tests (expect fail if field still required without default)**

```bash
cd /Users/antonbutov/Documents/MYPROJECTS/fixaverse/client-app
./gradlew :auth:jvmTest --tests 'pro.masterdoc.client.auth.*' :shared:jvmTest --tests 'pro.masterdoc.client.session.ClientSessionFromMeTest'
```

- [ ] **Step 3: Remove `roles` from `UserInfoDto`; fix logs/fixtures**

```kotlin
@Serializable
data class UserInfoDto(
    val id: String,
    val givenName: String? = null,
    val familyName: String? = null,
    val email: String? = null,
)
```

`FeatureSetFixtures` example:

```kotlin
object FeatureSetFixtures {
    fun board(): Set<FeatureId> = setOf(FeatureId.Board, FeatureId.Profile)
    fun usersAdmin(): Set<FeatureId> = setOf(FeatureId.Users, FeatureId.Profile)
}
```

- [ ] **Step 4: Re-run tests — PASS**

- [ ] **Step 5: Commit** (in `client-app`)

```bash
git add auth shared
git commit -m "feat: drop roles from client /me DTO"
```

---

### Task 6: client-app — Admin users API (list + invite)

**Files:**
- Create: `auth/src/commonMain/kotlin/pro/masterdoc/client/auth/AdminUserModels.kt`
- Modify: `auth/src/commonMain/kotlin/pro/masterdoc/client/auth/AuthRepository.kt` **or** create `AdminUsersRepository.kt` next to it (prefer separate repo class)
- Modify: `auth/src/commonMain/kotlin/pro/masterdoc/client/auth/AuthModule.kt` (Koin bind)
- Create: `auth/src/jvmTest/kotlin/pro/masterdoc/client/auth/AdminUsersRepositoryTest.kt`

**Interfaces:**
- Consumes: `GatewayHttpClient`, `AuthConfig.gatewayBaseUrl`, access token from `TokenStore`
- Produces:
  - `suspend fun listUsers(limit: Int = 50, offset: Int = 0): AdminUserList`
  - `suspend fun inviteUser(request: InviteUserRequest): AdminUser`
  - models mirroring gateway (`features: List<String>`)

- [ ] **Step 1: Failing repository tests with FakeGatewayHttpClient**

```kotlin
@Test
fun inviteUser_postsFeaturesBody() = runBlocking {
    val http = FakeGatewayHttpClient { method, url, headers, body ->
        assertEquals("POST", method)
        assertTrue(url.endsWith("/admin/users/invites"))
        assertTrue(body!!.contains("\"features\""))
        assertTrue(body.contains("board"))
        GatewayHttpResponse(201, """{"id":"1","email":"a@b.com","givenName":"A","familyName":"B","features":["board"],"state":"invited","inviteSent":true}""")
    }
    // token store with access token; call inviteUser(...)
}
```

- [ ] **Step 2: Run test — FAIL (missing type/method)**

```bash
./gradlew :auth:jvmTest --tests pro.masterdoc.client.auth.AdminUsersRepositoryTest
```

- [ ] **Step 3: Implement models + repository**

```kotlin
@Serializable
data class InviteUserRequest(
    val email: String,
    val givenName: String,
    val familyName: String,
    val features: List<String>,
)

@Serializable
data class AdminUser(
    val id: String,
    val email: String,
    val givenName: String,
    val familyName: String,
    val features: List<String>,
    val state: String,
    val inviteSent: Boolean? = null,
)

@Serializable
data class AdminUserList(val items: List<AdminUser>, val total: Int)
```

POST/GET with `Authorization: Bearer …` like existing `fetchMe`.

- [ ] **Step 4: Tests PASS**

- [ ] **Step 5: Commit**

```bash
git add auth
git commit -m "feat: add admin users list and invite API client"
```

---

### Task 7: client-app — Users screen (list + invite by email + features)

**Files:**
- Create: `shared/src/commonMain/kotlin/pro/masterdoc/client/presentation/users/UsersComponent.kt` (state: list, invite form fields, selected features, error/loading)
- Create: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/screens/UsersScreen.kt`
- Modify: `shared/src/commonMain/kotlin/pro/masterdoc/client/presentation/shell/MainShellComponent.kt` — `PageChild.Users(UsersComponent)` instead of Stub for Users
- Modify: `composeApp/src/commonMain/kotlin/pro/masterdoc/client/ui/shell/MainShellContent.kt` — render `UsersScreen`
- Create: `shared/src/commonTest/kotlin/pro/masterdoc/client/presentation/users/InviteFormValidatorTest.kt` (pure validation)
- Wire Koin/factory so `UsersComponent` gets `AdminUsersRepository`

**Interfaces:**
- Consumes: `AdminUsersRepository.listUsers`, `inviteUser`
- Produces: UI copy in Russian without слово «роль»; checkboxes for catalog wires; submit requires non-empty features + email

- [ ] **Step 1: Failing pure tests for invite validation**

```kotlin
@Test
fun rejectsEmptyFeatures() {
    assertEquals(
        InviteFormError.FeaturesRequired,
        InviteFormValidator.validate(email = "a@b.com", given = "A", family = "B", features = emptySet()),
    )
}

@Test
fun acceptsValid() {
    assertEquals(
        null,
        InviteFormValidator.validate(email = "a@b.com", given = "A", family = "B", features = setOf("board")),
    )
}
```

- [ ] **Step 2: Run — FAIL**

```bash
./gradlew :shared:jvmTest --tests pro.masterdoc.client.presentation.users.InviteFormValidatorTest
```

- [ ] **Step 3: Implement validator + component + screen**

Grantable wires for UI (exclude nothing from catalog — include `user_invite`):

```kotlin
val GrantableFeatures = listOf("board", "copilot", "charts", "equipment", "user_invite")
```

`UsersScreen`: list (`LazyColumn` of email + features chips), form fields, multi-select, button «Пригласить». On success refresh list. Errors: show message for 400/403/409/502.

`MainShellComponent.PageChild`:

```kotlin
data class Users(val component: UsersComponent) : PageChild {
    override val destination = NavDestinationId.Users
}
```

Factory: when destination is Users, create `DefaultUsersComponent`.

- [ ] **Step 4: Run shared jvmTest for users + existing nav tests**

```bash
./gradlew :shared:jvmTest --tests 'pro.masterdoc.client.presentation.users.*' --tests 'pro.masterdoc.client.navigation.*'
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared composeApp
git commit -m "feat: Users screen list and invite with feature set"
```

---

### Task 8: Docs sweep + push for CI

**Files:** any remaining README/OpenAPI comments in the four repos that still say product “roles” for `/me` or invite.

- [ ] **Step 1: Grep and fix**

```bash
rg -n '\broles?\b' feature-service/README.md api-gateway-service/README.md api-gateway-service/openapi.yaml client-app/README.md masterdoc-zitadel/docs -i
```

Replace product language; leave Zitadel API `roleKeys` code comments that clarify IdP field names.

- [ ] **Step 2: Commit per repo if dirty**

- [ ] **Step 3: Push branches** (when user asks / as part of execution) and watch CI — do **not** run local Wasm/desktop distribution builds.

---

## Self-review (plan vs spec)

| Spec requirement | Task |
|------------------|------|
| `/me` without `roles`; features from grants | Task 1 |
| Catalog filter; no role→feature map | Task 1 |
| Zitadel keys = wire features | Task 2 |
| Admin invite/list/`PUT …/features` | Tasks 3–4 |
| OpenAPI/docs features language | Tasks 4, 8 |
| Client DTO without roles | Task 5 |
| Users list + invite by email + features | Tasks 6–7 |
| TDD | Each task Steps 1–4 |
| `profile` client-only | Task 5 (unchanged `fromMe`) |
| No production grant migration script | Out of scope (spec) |

No TBD placeholders. Types: `features: List<String>` consistently; gateway `SetFeaturesRequest`; client `InviteUserRequest.features`.
