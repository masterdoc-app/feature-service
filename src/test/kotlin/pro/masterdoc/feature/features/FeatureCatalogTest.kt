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
        assertEquals(emptyList<String>(), catalog.filter(listOf("user_invite", "dispatcher")))
    }

    @Test
    fun `catalog returns all features with russian titles`() {
        val items = catalog.catalog()
        assertEquals(5, items.size)
        assertEquals("admin", items.first().id)
        assertEquals("Админ", items.first().titleRu)
        assertEquals(
            listOf("admin", "board", "charts", "copilot", "equipment"),
            items.map { it.id },
        )
        assertEquals(
            listOf("Админ", "Доска", "ППР", "Наставник", "Оборудование"),
            items.map { it.titleRu },
        )
    }
}
