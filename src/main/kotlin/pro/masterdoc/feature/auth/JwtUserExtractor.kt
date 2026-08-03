package pro.masterdoc.feature.auth

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component

data class JwtUserClaims(
    val id: String,
    val givenName: String?,
    val familyName: String?,
    val email: String?,
    val orgName: String?,
    val grantKeys: List<String>,
)

@Component
class JwtUserExtractor {

    fun extract(jwt: Jwt): JwtUserClaims {
        return JwtUserClaims(
            id = jwt.subject.orEmpty(),
            givenName = jwt.getClaimAsString("given_name"),
            familyName = jwt.getClaimAsString("family_name"),
            email = jwt.getClaimAsString("email"),
            orgName = jwt.getClaimAsString(ORG_NAME_CLAIM)?.trim()?.takeIf { it.isNotEmpty() },
            grantKeys = extractGrantKeys(jwt),
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractGrantKeys(jwt: Jwt): List<String> {
        val projectRoles = jwt.claims["urn:zitadel:iam:org:project:roles"]
        if (projectRoles is Map<*, *>) {
            return projectRoles.keys.mapNotNull { it as? String }.sorted()
        }
        val rolesClaim = jwt.claims["roles"]
        if (rolesClaim is Collection<*>) {
            return rolesClaim.mapNotNull { it as? String }.sorted()
        }
        return emptyList()
    }

    companion object {
        const val ORG_NAME_CLAIM = "urn:zitadel:iam:user:resourceowner:name"
    }
}
