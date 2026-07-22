package pro.masterdoc.feature.features

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class FeatureCatalogTest {
    private val catalog = FeatureCatalog()

    @Test
    fun `keeps known grant keys sorted`() {
        assertEquals(listOf("board", "charts"), catalog.filter(listOf("charts", "board", "unknown")))
    }

    @Test
    fun `drops unknown keys`() {
        assertEquals(emptyList<String>(), catalog.filter(listOf("admin", "dispatcher")))
    }

    @Test
    fun `catalog returns all features with russian titles`() {
        val items = catalog.catalog()
        assertEquals(5, items.size)
        assertEquals("board", items.first().id)
        assertEquals("Доска", items.first().titleRu)
        assertEquals(
            listOf("board", "charts", "copilot", "equipment", "user_invite"),
            items.map { it.id },
        )
        assertEquals(
            listOf("Доска", "Графики", "Наставник", "Оборудование", "Пользователи"),
            items.map { it.titleRu },
        )
    }
}
