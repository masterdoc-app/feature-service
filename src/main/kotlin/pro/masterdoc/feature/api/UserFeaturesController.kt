package pro.masterdoc.feature.api

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException
import pro.masterdoc.feature.features.FeatureCatalog
import pro.masterdoc.feature.grants.UserGrantsLookup

data class UserFeaturesResponse(
    val features: List<String>,
)

/**
 * Service-to-service lookup of a **target** user's catalog features (not the caller's JWT).
 * Used by dashboard when enforcing WO assignee eligibility (`equipment`).
 */
@RestController
class UserFeaturesController(
    private val userGrantsLookup: UserGrantsLookup,
    private val featureCatalog: FeatureCatalog,
) {
    @GetMapping("/users/{userId}/features")
    fun getUserFeatures(
        @PathVariable userId: String,
        @RequestHeader("X-Org-Id", required = false) orgIdHeader: String?,
    ): UserFeaturesResponse {
        val orgId = orgIdHeader?.takeIf { it.isNotBlank() }
            ?: throw ResponseStatusException(HttpStatus.BAD_REQUEST, "X-Org-Id required")
        val features = featureCatalog.filter(userGrantsLookup.grantKeys(orgId, userId))
        return UserFeaturesResponse(features = features)
    }
}
