package pro.masterdoc.feature.api

import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.`when`
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
        "internal.service-token=test-internal-token",
    ],
)
class UserFeaturesControllerTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var userGrantsLookup: UserGrantsLookup

    @Test
    fun `getUserFeatures rejects without internal token`() {
        mockMvc.get("/users/engineer-1/features") {
            header("X-Org-Id", "org-1")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `getUserFeatures rejects wrong internal token`() {
        mockMvc.get("/users/engineer-1/features") {
            header("X-Org-Id", "org-1")
            header(InternalServiceTokenFilter.HEADER, "wrong")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun `getUserFeatures returns catalog-filtered grants for target user`() {
        `when`(userGrantsLookup.grantKeys("org-1", "engineer-1"))
            .thenReturn(listOf("equipment", "board", "copilot"))

        mockMvc.get("/users/engineer-1/features") {
            header("X-Org-Id", "org-1")
            header(InternalServiceTokenFilter.HEADER, "test-internal-token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.features[0]") { value("board") }
            jsonPath("$.features[1]") { value("equipment") }
            jsonPath("$.features.length()") { value(2) }
        }
    }

    @Test
    fun `getUserFeatures requires org header`() {
        mockMvc.get("/users/engineer-1/features") {
            header(InternalServiceTokenFilter.HEADER, "test-internal-token")
        }.andExpect {
            status { isBadRequest() }
        }
    }

    @Test
    fun `getUserFeatures board-only has no equipment`() {
        `when`(userGrantsLookup.grantKeys(anyString(), anyString())).thenReturn(listOf("board"))

        mockMvc.get("/users/dispatcher-1/features") {
            header("X-Org-Id", "org-1")
            header(InternalServiceTokenFilter.HEADER, "test-internal-token")
        }.andExpect {
            status { isOk() }
            jsonPath("$.features[0]") { value("board") }
            jsonPath("$.features.length()") { value(1) }
        }
    }
}
