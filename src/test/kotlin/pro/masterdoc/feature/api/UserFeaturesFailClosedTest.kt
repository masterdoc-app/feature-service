package pro.masterdoc.feature.api

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import pro.masterdoc.feature.config.InternalServiceTokenFilter
import pro.masterdoc.feature.grants.UserGrantsLookup

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(
    properties = [
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=",
        "spring.security.oauth2.resourceserver.jwt.jwk-set-uri=",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.oauth2.resource.servlet.OAuth2ResourceServerAutoConfiguration",
        "internal.service-token=",
    ],
)
class UserFeaturesFailClosedTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var userGrantsLookup: UserGrantsLookup

    @Test
    fun `getUserFeatures rejects when internal token not configured`() {
        mockMvc.get("/users/engineer-1/features") {
            header("X-Org-Id", "org-1")
            header(InternalServiceTokenFilter.HEADER, "anything")
        }.andExpect {
            status { isUnauthorized() }
        }
    }
}
