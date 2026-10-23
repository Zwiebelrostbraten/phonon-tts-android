package ai.gradium.phonon.tts
import org.junit.Assert.*
import org.junit.Test
class SettingsStateTest {
    @Test fun initialSelectionRequiresUniqueMatch() {
        assertEquals(0, initialModelSelection(listOf(false, false, false)))
        assertEquals(1, initialModelSelection(listOf(false, true, false)))
        assertEquals(0, initialModelSelection(listOf(true, true, false)))
    }

    @Test fun replacementCancelNeverInstalls() {
        val gate = ReplacementGate()
        var installs = 0
        gate.request(true, false) { installs++ }
        assertTrue(gate.pending)
        gate.cancel()
        gate.accept(false)
        assertEquals(0, installs)
    }
    @Test fun acceptanceConsumesActionOnce() {
        val gate = ReplacementGate()
        var installs = 0
        gate.request(true, false) { installs++ }
        gate.accept(false); gate.accept(false)
        assertEquals(1, installs)
    }
    @Test fun busyBlocksRequestAndAcceptance() {
        val gate = ReplacementGate()
        var installs = 0
        gate.request(false, true) { installs++ }
        gate.request(true, false) { installs++ }
        gate.accept(true)
        assertEquals(0, installs)
        assertFalse(gate.pending)
    }
    @Test fun newInstallRunsOnlyOnExplicitRequest() {
        val gate = ReplacementGate()
        var installs = 0
        gate.request(false, false) { installs++ }
        assertEquals(1, installs)
        gate.accept(false)
        assertEquals(1, installs)
    }
}
