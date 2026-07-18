package pro.masterdoc.feature.features

import org.springframework.stereotype.Component

@Component
class RoleFeatureResolver {

    fun resolve(roles: List<String>): List<String> {
        val features = linkedSetOf<String>()
        for (role in roles) {
            when (role) {
                "dispatcher" -> features.add("board")
                "engineer" -> features.add("copilot")
            }
        }
        return features.toList()
    }
}
