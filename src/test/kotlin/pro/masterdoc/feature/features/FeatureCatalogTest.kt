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
}
