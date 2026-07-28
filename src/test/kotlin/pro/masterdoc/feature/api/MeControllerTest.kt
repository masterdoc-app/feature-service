package pro.masterdoc.feature.api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
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
            jsonPath("$.userInfo.familyName") { value("Petrov") }
            jsonPath("$.userInfo.email") { value("ivan@example.com") }
            jsonPath("$.userInfo.roles") { doesNotExist() }
            jsonPath("$.features[0]") { value("board") }
        }
    }

    @Test
    fun `getMe drops stale copilot grant from features`() {
        val token = Jwt.withTokenValue("test-token")
            .header("alg", "none")
            .subject("user-2")
            .claim(
                "urn:zitadel:iam:org:project:roles",
                mapOf("copilot" to mapOf("project-id" to "zitadel.localhost")),
            )
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))
            .build()

        mockMvc.get("/me") {
            with(jwt().jwt(token))
        }.andExpect {
            status { isOk() }
            jsonPath("$.features") { isEmpty() }
            jsonPath("$.userInfo.roles") { doesNotExist() }
        }
    }
}
