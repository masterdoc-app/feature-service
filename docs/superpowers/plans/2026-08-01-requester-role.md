# Requester Role Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add product role `requester` («Заявитель») with default feature `tickets`, including backfill for orgs that already have other roles seeded.

**Architecture:** Extend `ProductRoleService` defaults and `listWithSeed` so empty orgs get five roles, and non-empty orgs get any missing default roles inserted without changing existing rows. Gateway and client already proxy/list roles from the API — no changes there.

**Tech Stack:** Kotlin, Spring Boot, MockMvc tests, Gradle, PostgreSQL via existing `product_roles` table.

## Global Constraints

- roleId: `requester`; titleRu: `Заявитель`; default features: `["tickets"]` only.
- Do not remove or alter defaults of `admin`, `dispatcher`, `engineer`, `manager`.
- Backfill inserts only missing role rows; never overwrite existing role features/titles.
- Feature `tickets` already exists in `FeatureCatalog` — do not add a new catalog entry.
- No client-app / gateway changes in this plan.
- Prefer targeted Gradle test (`RolesControllerTest`); full suite via GitHub Actions after push (Fixaverse: no heavy local builds).
- After each commit: push and watch Actions.

---

## File map

| File | Responsibility |
|------|----------------|
| `src/main/kotlin/pro/masterdoc/feature/roles/ProductRoleService.kt` | `ROLE_IDS`, `DEFAULTS`, `listWithSeed` seed + backfill |
| `src/main/kotlin/pro/masterdoc/feature/roles/ProductRoleRepository.kt` | existing `insertAll` / `list` / `count` — reuse; no new methods unless needed |
| `src/test/kotlin/pro/masterdoc/feature/api/RolesControllerTest.kt` | seed count, requester defaults, backfill, PUT requester |

---

### Task 1: Seed `requester` for new orgs

**Files:**
- Modify: `src/test/kotlin/pro/masterdoc/feature/api/RolesControllerTest.kt`
- Modify: `src/main/kotlin/pro/masterdoc/feature/roles/ProductRoleService.kt`

**Interfaces:**
- Consumes: `ProductRoleRepository.insertAll`, `ProductRoleRepository.list`, `ProductRoleRepository.count`
- Produces: `DEFAULTS` includes `ProductRole("requester", "Заявитель", listOf("tickets"))`; `ROLE_IDS` includes `"requester"`

- [ ] **Step 1: Update failing seed test**

In `RolesControllerTest`, change `empty org GET seeds four roles` to expect five roles and assert requester:

```kotlin
@Test
fun `empty org GET seeds five roles including requester`() {
    mockMvc.get("/roles") {
        with(jwtRequest())
        header("X-Org-Id", "seed-org")
    }.andExpect {
        status { isOk() }
        jsonPath("$.items.length()") { value(5) }
        jsonPath("$.items[0].id") { value("admin") }
        jsonPath("$.items[0].features[0]") { value("admin") }
        jsonPath("$.items[4].id") { value("requester") }
        jsonPath("$.items[4].titleRu") { value("Заявитель") }
        jsonPath("$.items[4].features[0]") { value("tickets") }
        jsonPath("$.items[4].features.length()") { value(1) }
    }
}
```

Note: `ORDER BY role_id` → order is `admin`, `dispatcher`, `engineer`, `manager`, `requester` (index 4).

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew test --tests pro.masterdoc.feature.api.RolesControllerTest`

Expected: FAIL — length still 4 and/or no `requester`.

- [ ] **Step 3: Minimal implementation — defaults + ROLE_IDS**

In `ProductRoleService` companion:

```kotlin
private val ROLE_IDS = setOf("admin", "dispatcher", "engineer", "manager", "requester")
private val DEFAULTS =
    listOf(
        ProductRole("admin", "Админ", listOf("admin", "black_box", "equipment")),
        ProductRole("dispatcher", "Диспетчер", listOf("map", "board", "ai", "charts")),
        ProductRole("engineer", "Инженер", listOf("engineer")),
        ProductRole("manager", "Менеджер", listOf("reports")),
        ProductRole("requester", "Заявитель", listOf("tickets")),
    )
```

Leave `listWithSeed` as empty-only seed for now (Task 2 adds backfill).

- [ ] **Step 4: Run test to verify seed passes**

Run: `./gradlew test --tests pro.masterdoc.feature.api.RolesControllerTest`

Expected: new seed test PASS; other existing tests PASS.

- [ ] **Step 5: Commit and push**

```bash
git add src/main/kotlin/pro/masterdoc/feature/roles/ProductRoleService.kt \
  src/test/kotlin/pro/masterdoc/feature/api/RolesControllerTest.kt
git commit -m "$(cat <<'EOF'
feat(roles): seed requester role with tickets feature

EOF
)"
git push
```

Watch: `gh run watch` on the triggered CI run; report result.

---

### Task 2: Backfill `requester` for existing orgs + PUT works

**Files:**
- Modify: `src/main/kotlin/pro/masterdoc/feature/roles/ProductRoleService.kt` (`listWithSeed`)
- Modify: `src/test/kotlin/pro/masterdoc/feature/api/RolesControllerTest.kt`

**Interfaces:**
- Consumes: `ProductRoleRepository.list`, `insertAll`, `count`
- Produces: `listWithSeed` inserts any `DEFAULTS` roles missing by `roleId` without mutating existing rows

- [ ] **Step 1: Write failing backfill + PUT tests**

Add `@Autowired JdbcTemplate` and these tests to `RolesControllerTest`:

```kotlin
@Autowired
private lateinit var jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate

@Test
fun `existing org without requester gets backfilled on GET`() {
    val orgId = "backfill-org"
    jdbcTemplate.update(
        "INSERT INTO product_roles (org_id, role_id, title_ru, features) VALUES (?, ?, ?, ?)",
        orgId,
        "admin",
        "Админ",
        """["admin","black_box","equipment"]""",
    )
    jdbcTemplate.update(
        "INSERT INTO product_roles (org_id, role_id, title_ru, features) VALUES (?, ?, ?, ?)",
        orgId,
        "dispatcher",
        "Диспетчер",
        """["map","board","ai","charts"]""",
    )
    jdbcTemplate.update(
        "INSERT INTO product_roles (org_id, role_id, title_ru, features) VALUES (?, ?, ?, ?)",
        orgId,
        "engineer",
        "Инженер",
        """["engineer"]""",
    )
    jdbcTemplate.update(
        "INSERT INTO product_roles (org_id, role_id, title_ru, features) VALUES (?, ?, ?, ?)",
        orgId,
        "manager",
        "Менеджер",
        """["reports"]""",
    )

    mockMvc.get("/roles") {
        with(jwtRequest())
        header("X-Org-Id", orgId)
    }.andExpect {
        status { isOk() }
        jsonPath("$.items.length()") { value(5) }
        jsonPath("$.items[4].id") { value("requester") }
        jsonPath("$.items[4].features[0]") { value("tickets") }
        // existing manager unchanged
        jsonPath("$.items[3].id") { value("manager") }
        jsonPath("$.items[3].features[0]") { value("reports") }
        jsonPath("$.items[3].features.length()") { value(1) }
    }
}

@Test
fun `PUT requester updates features`() {
    mockMvc.put("/roles/requester") {
        with(jwtRequest())
        header("X-Org-Id", "requester-update-org")
        contentType = org.springframework.http.MediaType.APPLICATION_JSON
        content = """{"features":["tickets","map"]}"""
    }.andExpect {
        status { isOk() }
        jsonPath("$.id") { value("requester") }
        jsonPath("$.titleRu") { value("Заявитель") }
        jsonPath("$.features[0]") { value("map") }
        jsonPath("$.features[1]") { value("tickets") }
    }
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests pro.masterdoc.feature.api.RolesControllerTest`

Expected: backfill test FAIL (length 4); PUT may 404 if `requester` not in `ROLE_IDS` yet — after Task 1 ROLE_IDS already has it, so PUT should 200 after seed; backfill is the main failure.

- [ ] **Step 3: Implement backfill in `listWithSeed`**

Replace body of `listWithSeed` with:

```kotlin
@Transactional
fun listWithSeed(orgId: String): List<ProductRole> {
    requireOrgId(orgId)
    if (repository.count(orgId) == 0) {
        repository.insertAll(orgId, DEFAULTS)
    } else {
        val existingIds = repository.list(orgId).map { it.roleId }.toSet()
        val missing = DEFAULTS.filter { it.roleId !in existingIds }
        if (missing.isNotEmpty()) {
            repository.insertAll(orgId, missing)
        }
    }
    return repository.list(orgId)
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `./gradlew test --tests pro.masterdoc.feature.api.RolesControllerTest`

Expected: all tests in class PASS.

- [ ] **Step 5: Commit and push**

```bash
git add src/main/kotlin/pro/masterdoc/feature/roles/ProductRoleService.kt \
  src/test/kotlin/pro/masterdoc/feature/api/RolesControllerTest.kt
git commit -m "$(cat <<'EOF'
feat(roles): backfill requester role for existing orgs

EOF
)"
git push
```

Watch CI to success.

---

## Spec coverage checklist

| Spec requirement | Task |
|------------------|------|
| Default role requester / Заявитель / tickets | Task 1 |
| New org seeds five roles | Task 1 |
| Existing org backfills missing requester | Task 2 |
| PUT /roles/requester works | Task 2 |
| No catalog / client / gateway changes | (none) |
