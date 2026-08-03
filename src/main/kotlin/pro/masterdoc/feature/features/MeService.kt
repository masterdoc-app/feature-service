package pro.masterdoc.feature.features

import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.stereotype.Service
import pro.masterdoc.feature.api.MeResponse
import pro.masterdoc.feature.api.UserInfoDto
import pro.masterdoc.feature.auth.JwtUserExtractor

@Service
class MeService(
    private val jwtUserExtractor: JwtUserExtractor,
    private val featureCatalog: FeatureCatalog,
) {
    private val log = LoggerFactory.getLogger(MeService::class.java)

    fun getMe(jwt: Jwt): MeResponse {
        val claims = jwtUserExtractor.extract(jwt)
        val features = featureCatalog.filter(claims.grantKeys)
        log.debug("event=me_ok userId={} featureCount={}", claims.id, features.size)
        return MeResponse(
            userInfo = UserInfoDto(
                id = claims.id,
                givenName = claims.givenName,
                familyName = claims.familyName,
                email = claims.email,
                orgName = claims.orgName,
            ),
            features = features,
        )
    }
}

