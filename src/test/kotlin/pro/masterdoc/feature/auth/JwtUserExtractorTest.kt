package pro.masterdoc.feature.auth

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant

class JwtUserExtractorTest {
    private val extractor = JwtUserExtractor()

    @Test
    fun `extracts sorted grant keys from project roles claim`() {
        val jwt = jwt(
            subject = "user-1",
            claims = mapOf(
                "urn:zitadel:iam:org:project:roles" to mapOf(
                    "charts" to mapOf("project-id" to "zitadel.localhost"),
                    "board" to mapOf("project-id" to "zitadel.localhost"),
                ),
            ),
        )

        assertEquals(listOf("board", "charts"), extractor.extract(jwt).grantKeys)
    }

    @Test
    fun `falls back to sorted roles collection claim`() {
        val jwt = jwt(
            subject = "user-1",
            claims = mapOf("roles" to listOf("charts", "board")),
        )

        assertEquals(listOf("board", "charts"), extractor.extract(jwt).grantKeys)
    }

    @Test
    fun `returns empty grant keys when neither claim is present`() {
        val jwt = jwt(subject = "user-1")

        assertEquals(emptyList<String>(), extractor.extract(jwt).grantKeys)
    }

    @Test
    fun `uses empty id for null or empty subject`() {
        val nullSubject = jwt(claims = emptyMap())
        val emptySubject = jwt(subject = "")

        assertEquals("", extractor.extract(nullSubject).id)
        assertEquals("", extractor.extract(emptySubject).id)
    }

    @Test
    fun `maps given name family name and email claims`() {
        val jwt = jwt(
            subject = "user-1",
            claims = mapOf(
                "given_name" to "Ivan",
                "family_name" to "Petrov",
                "email" to "ivan@example.com",
            ),
        )

        val claims = extractor.extract(jwt)
        assertEquals("user-1", claims.id)
        assertEquals("Ivan", claims.givenName)
        assertEquals("Petrov", claims.familyName)
        assertEquals("ivan@example.com", claims.email)
    }

    private fun jwt(
        subject: String? = null,
        claims: Map<String, Any> = emptyMap(),
    ): Jwt {
        var builder = Jwt.withTokenValue("t")
            .header("alg", "none")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(3600))

        if (subject != null) {
            builder = builder.subject(subject)
        }

        claims.forEach { (name, value) ->
            builder = builder.claim(name, value)
        }

        return builder.build()
    }
}
