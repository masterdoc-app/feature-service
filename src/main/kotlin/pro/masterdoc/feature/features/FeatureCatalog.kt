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
                FeatureDefinition("ai", "ИИ"),
                FeatureDefinition("black_box", "Чёрный ящик"),
                FeatureDefinition("board", "Доска"),
                FeatureDefinition("charts", "ППР"),
                FeatureDefinition("engineer", "Инженер"),
                FeatureDefinition("equipment", "Оборудование"),
                FeatureDefinition("map", "Карта"),
                FeatureDefinition("tickets", "Заявки"),
            )

        val ALL: Set<String> = ENTRIES.map { it.id }.toSet()
    }

    fun catalog(): List<FeatureDefinition> = ENTRIES.sortedBy { it.id }

    fun filter(grantKeys: List<String>): List<String> =
        grantKeys
            .map { if (it == "user_invite") "admin" else it }
            .filter { it in ALL }
            .toSortedSet()
            .toList()
}
