package pro.masterdoc.feature.features

import org.springframework.stereotype.Component

@Component
class FeatureCatalog {
    companion object {
        val ALL: Set<String> = setOf("board", "copilot", "charts", "equipment", "user_invite")
    }

    fun filter(grantKeys: List<String>): List<String> =
        grantKeys.filter { it in ALL }.toSortedSet().toList()
}
