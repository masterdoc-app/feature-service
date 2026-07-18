package pro.masterdoc.feature.auth

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Component
import pro.masterdoc.feature.api.UserInfoDto

@Component
class JwtUserExtractor {

    fun extract(jwt: Jwt): UserInfoDto {
        val roles = extractRoles(jwt)
        return UserInfoDto(
            id = jwt.subject.orEmpty(),
            givenName = jwt.getClaimAsString("given_name"),
            familyName = jwt.getClaimAsString("family_name"),
            email = jwt.getClaimAsString("email"),
            roles = roles,
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun extractRoles(jwt: Jwt): List<String> {
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
}
