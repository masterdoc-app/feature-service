package pro.masterdoc.feature.features

import org.springframework.stereotype.Component

data class FeatureDefinition(
    val id: String,
    val titleRu: String,
)

@Component
class FeatureCatalog {
    companion object {
        val ENTRIES: List<FeatureDefinition> =
            listOf(
                FeatureDefinition("admin", "Админ"),
                FeatureDefinition("board", "Доска"),
                FeatureDefinition("charts", "ППР"),
                FeatureDefinition("copilot", "Наставник"),
                FeatureDefinition("equipment", "Оборудование"),
            )

        val ALL: Set<String> = ENTRIES.map { it.id }.toSet()
    }

    fun catalog(): List<FeatureDefinition> = ENTRIES.sortedBy { it.id }

    fun filter(grantKeys: List<String>): List<String> =
        grantKeys.filter { it in ALL }.toSortedSet().toList()
}
