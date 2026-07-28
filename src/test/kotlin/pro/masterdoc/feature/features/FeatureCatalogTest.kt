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
        assertEquals(emptyList<String>(), catalog.filter(listOf("dispatcher", "legacy_admin")))
    }

    @Test
    fun `maps legacy user_invite grant to admin`() {
        assertEquals(listOf("admin", "board"), catalog.filter(listOf("user_invite", "board")))
    }

    @Test
    fun `catalog returns all features with russian titles`() {
        val items = catalog.catalog()
        assertEquals(5, items.size)
        assertEquals("admin", items.first().id)
        assertEquals("Админ", items.first().titleRu)
        assertEquals(
            listOf("admin", "black_box", "board", "charts", "equipment"),
            items.map { it.id },
        )
        assertEquals(
            listOf("Админ", "Чёрный ящик", "Доска", "ППР", "Оборудование"),
            items.map { it.titleRu },
        )
    }

    @Test
    fun `drops stale copilot grant`() {
        assertEquals(listOf("equipment"), catalog.filter(listOf("copilot", "equipment")))
    }

    @Test
    fun `includes black_box definition`() {
        val item = catalog.catalog().single { it.id == "black_box" }
        assertEquals("Чёрный ящик", item.titleRu)
    }
}
