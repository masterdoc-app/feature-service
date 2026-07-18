package pro.masterdoc.feature.features

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class RoleFeatureResolverTest {

    private val resolver = RoleFeatureResolver()

    @Test
    fun `dispatcher role gets board feature`() {
        val features = resolver.resolve(listOf("dispatcher"))
        assertTrue(features.contains("board"))
    }

    @Test
    fun `engineer role gets copilot feature`() {
        val features = resolver.resolve(listOf("engineer"))
        assertTrue(features.contains("copilot"))
    }
}
