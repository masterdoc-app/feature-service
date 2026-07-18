package pro.masterdoc.feature.api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import java.time.Instant

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
    ],
)
class MeControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `getMe for dispatcher returns board feature and userInfo`() {
        val token = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject("user-1")
            .claim("given_name", "Ivan")
            .claim("family_name", "Petrov")
            .claim("email", "ivan@example.com")
            .claim(
                "urn:zitadel:iam:org:project:roles",
                mapOf("dispatcher" to mapOf("project-id" to "zitadel.localhost")),
            )
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        mockMvc.get("/me") {
            with(
                jwt()
                    .jwt(token)
                    .authorities(SimpleGrantedAuthority("ROLE_dispatcher")),
            )
        }.andExpect {
            status { isOk() }
            jsonPath("$.userInfo.id") { value("user-1") }
            jsonPath("$.userInfo.givenName") { value("Ivan") }
            jsonPath("$.userInfo.familyName") { value("Petrov") }
            jsonPath("$.userInfo.email") { value("ivan@example.com") }
            jsonPath("$.features[0]") { value("board") }
        }
    }

    @Test
    fun `getMe for engineer returns copilot feature`() {
        val token = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject("user-2")
            .claim(
                "urn:zitadel:iam:org:project:roles",
                mapOf("engineer" to mapOf("project-id" to "zitadel.localhost")),
            )
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        mockMvc.get("/me") {
            with(jwt().jwt(token))
        }.andExpect {
            status { isOk() }
            jsonPath("$.features[0]") { value("copilot") }
        }
    }
}
