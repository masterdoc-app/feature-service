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
    fun `empty org GET seeds four roles`() {
        mockMvc.get("/roles") {
            with(jwtRequest())
            header("X-Org-Id", "seed-org")
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(4) }
            jsonPath("$.items[0].id") { value("admin") }
            jsonPath("$.items[0].features[0]") { value("admin") }
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
