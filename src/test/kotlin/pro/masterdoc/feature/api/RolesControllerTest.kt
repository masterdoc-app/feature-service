package pro.masterdoc.feature.api

import java.time.Instant
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.put

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
    ],
)
class RolesControllerTest {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var jdbcTemplate: org.springframework.jdbc.core.JdbcTemplate

    private fun jwtRequest() =
        jwt().jwt(
            Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build(),
        )

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
            jsonPath("$.items[0].features.length()") { value(3) }
            jsonPath("$.items[1].id") { value("dispatcher") }
            jsonPath("$.items[1].features") { value(org.hamcrest.Matchers.hasItem("equipment")) }
            jsonPath("$.items[1].features.length()") { value(5) }
            jsonPath("$.items[2].id") { value("engineer") }
            jsonPath("$.items[2].titleRu") { value("Инженер") }
            jsonPath("$.items[2].features[0]") { value("engineer") }
            jsonPath("$.items[2].features[1]") { value("tickets") }
            jsonPath("$.items[2].features.length()") { value(2) }
            jsonPath("$.items[4].id") { value("requester") }
            jsonPath("$.items[4].titleRu") { value("Заявитель") }
            jsonPath("$.items[4].features[0]") { value("tickets") }
            jsonPath("$.items[4].features.length()") { value(1) }
        }
    }

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
            jsonPath("$.items[0].id") { value("admin") }
            jsonPath("$.items[0].features[1]") { value("black_box") }
            jsonPath("$.items[0].features.length()") { value(3) }
            jsonPath("$.items[1].id") { value("dispatcher") }
            jsonPath("$.items[1].features") { value(org.hamcrest.Matchers.hasItem("equipment")) }
            jsonPath("$.items[1].features.length()") { value(5) }
            jsonPath("$.items[2].id") { value("engineer") }
            jsonPath("$.items[2].features") { value(org.hamcrest.Matchers.hasItem("tickets")) }
            jsonPath("$.items[2].features.length()") { value(2) }
            jsonPath("$.items[4].id") { value("requester") }
            jsonPath("$.items[4].features[0]") { value("tickets") }
            jsonPath("$.items[3].id") { value("manager") }
            jsonPath("$.items[3].features[0]") { value("reports") }
            jsonPath("$.items[3].features.length()") { value(1) }
        }
    }

    @Test
    fun `existing customized admin is not modified`() {
        val orgId = "custom-admin-org"
        jdbcTemplate.update(
            "INSERT INTO product_roles (org_id, role_id, title_ru, features) VALUES (?, ?, ?, ?)",
            orgId,
            "admin",
            "Старший админ",
            """["admin","equipment","map"]""",
        )

        mockMvc.get("/roles") {
            with(jwtRequest())
            header("X-Org-Id", orgId)
        }.andExpect {
            status { isOk() }
            jsonPath("$.items[0].id") { value("admin") }
            jsonPath("$.items[0].titleRu") { value("Старший админ") }
            jsonPath("$.items[0].features[0]") { value("admin") }
            jsonPath("$.items[0].features[1]") { value("equipment") }
            jsonPath("$.items[0].features[2]") { value("map") }
            jsonPath("$.items[0].features.length()") { value(3) }
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

    @Test
    fun `PUT unknown feature returns 400`() {
        mockMvc.put("/roles/admin") {
            with(jwtRequest())
            header("X-Org-Id", "invalid-feature-org")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"features":["unknown"]}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `PUT unknown role returns 404`() {
        mockMvc.put("/roles/unknown") {
            with(jwtRequest())
            header("X-Org-Id", "unknown-role-org")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"features":["admin"]}"""
        }.andExpect {
            status { isNotFound() }
        }
    }

    @Test
    fun `PUT updates role features`() {
        mockMvc.put("/roles/manager") {
            with(jwtRequest())
            header("X-Org-Id", "update-org")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"titleRu":"Руководитель","features":["reports","charts"]}"""
        }.andExpect {
            status { isOk() }
            jsonPath("$.id") { value("manager") }
            jsonPath("$.titleRu") { value("Руководитель") }
            jsonPath("$.features[0]") { value("charts") }
            jsonPath("$.features[1]") { value("reports") }
        }
    }

    @Test
    fun `PUT admin role without admin feature returns 400`() {
        mockMvc.put("/roles/admin") {
            with(jwtRequest())
            header("X-Org-Id", "admin-guard-org")
            contentType = org.springframework.http.MediaType.APPLICATION_JSON
            content = """{"features":["equipment","black_box"]}"""
        }.andExpect {
            status { isBadRequest() }
        }
    }
}
