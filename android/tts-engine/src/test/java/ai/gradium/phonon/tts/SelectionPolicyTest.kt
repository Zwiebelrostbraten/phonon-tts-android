package ai.gradium.phonon.tts

import org.junit.Assert.*
import org.junit.Test

class SelectionPolicyTest {
    private val installed = listOf(RouteModel("english", listOf("en")), RouteModel("multi", listOf("en", "de", "fr", "es", "pt")))
    private val current = ModelSelection("german", mapOf("de" to "german", "en" to "old"))

    @Test fun englishPreservesGermanDefault() {
        assertEquals(ModelSelection("english", mapOf("de" to "german", "en" to "english")), SelectionPolicy.choose(installed, "english", current))
    }
    @Test fun multilingualSetsAllFiveDefaults() {
        val result = SelectionPolicy.choose(installed, "multi", current)
        assertEquals("multi", result.selected)
        assertEquals(listOf("en", "de", "fr", "es", "pt").associateWith { "multi" }, result.defaults)
    }
    @Test fun unknownIdentityLeavesPreferencesUntouched() {
        assertSame(current, SelectionPolicy.choose(installed, "unknown", current))
    }
}
