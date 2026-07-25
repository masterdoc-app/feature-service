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
class FeaturesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Test
    fun `getFeatures returns catalog with russian titles`() {
        val token =
            Jwt.withTokenValue("test-token")
                .header("alg", "none")
                .subject("user-1")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .build()

        mockMvc.get("/features") {
            with(jwt().jwt(token))
        }.andExpect {
            status { isOk() }
            jsonPath("$.items.length()") { value(5) }
            jsonPath("$.items[0].id") { value("board") }
            jsonPath("$.items[0].titleRu") { value("Доска") }
            jsonPath("$.items[4].id") { value("user_invite") }
            jsonPath("$.items[4].titleRu") { value("Админ") }
        }
    }
}
