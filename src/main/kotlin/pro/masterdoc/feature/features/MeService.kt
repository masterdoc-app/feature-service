package pro.masterdoc.feature.features

import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import pro.masterdoc.feature.api.MeResponse
import pro.masterdoc.feature.auth.JwtUserExtractor

@Service
class MeService(
    private val jwtUserExtractor: JwtUserExtractor,
    private val roleFeatureResolver: RoleFeatureResolver,
) {
    fun getMe(jwt: Jwt): MeResponse {
        val userInfo = jwtUserExtractor.extract(jwt)
        val features = roleFeatureResolver.resolve(userInfo.roles)
        return MeResponse(userInfo = userInfo, features = features)
    }
}
