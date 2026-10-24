package ai.gradium.phonon.tts

import org.junit.Test
import org.junit.Assert.*
import java.io.File
import java.nio.file.Files
import java.security.MessageDigest
import java.util.concurrent.CancellationException

class VoicePackStoreTest {
    private val bytes = "verified voice".toByteArray()
    private fun voice(lang: String) = BundledVoice("Example", lang, "voices/id.safetensors", bytes.size.toLong(), DownloadPolicy.hex(MessageDigest.getInstance("SHA-256").digest(bytes)))
    private fun fill(dir: File) { File(dir,"voices").mkdirs(); File(dir,"voices/id.safetensors").writeBytes(bytes) }
    @Test fun canonicalCollisionsAreVerifiedInBothLocationsWithoutModification() {
        val root = Files.createTempDirectory("canonical").toFile()
        val entry = voice("de")
        try {
            assertFalse(VoicePackStore.existingCanonical(root, entry))
            for (folder in listOf("voices", "embeddings")) {
                val file = File(root, "$folder/${entry.stem}.safetensors")
                file.parentFile!!.mkdirs()
                file.writeBytes(bytes)
                assertTrue(VoicePackStore.existingCanonical(root, entry))
                // Same length, changed mtime: cached verification must recheck the SHA.
                file.writeBytes(ByteArray(bytes.size) { 1 })
                assertTrue(file.setLastModified(file.lastModified() + 2000))
                try { VoicePackStore.existingCanonical(root, entry); fail("Accepted corrupt $folder") }
                catch (_: IllegalStateException) { }
                assertArrayEquals(ByteArray(bytes.size) { 1 }, file.readBytes())
                file.writeText("short")
                try { VoicePackStore.existingCanonical(root, entry); fail("Accepted wrong size in $folder") }
                catch (_: IllegalStateException) { }
                assertEquals("short", file.readText())
                file.writeBytes(bytes)
                assertTrue(file.setLastModified(file.lastModified() + 4000))
                assertTrue(VoicePackStore.existingCanonical(root, entry))
            }
        } finally { root.deleteRecursively() }
    }
    @Test fun completeSharedLanguageAndExactHash() {
        val root = Files.createTempDirectory("packs").toFile()
        try {
            val store = VoicePackStore(root); val entries = listOf(voice("de"))
            assertFalse(store.ready("de", entries))
            store.install("de", entries, { false }, ::fill)
            assertTrue(VoicePackStore(root).ready("de", entries))
            File(root,"de/voices/id.safetensors").writeText("corrupt")
            assertFalse(store.ready("de", entries))
        } finally { root.deleteRecursively() }
    }
    @Test fun cancelledAndCorruptPreserveOtherLanguagesAndCheckpoint() {
        val root = Files.createTempDirectory("packs").toFile()
        try {
            val store = VoicePackStore(File(root,"packs")); val de = listOf(voice("de")); val fr = listOf(voice("fr"))
            val sentinel = File(root,"checkpoint").apply { writeText("untouched") }
            store.install("de", de, { false }, ::fill)
            store.install("fr", fr, { false }, ::fill)
            try { store.install("de", de, { true }, ::fill); fail() } catch (_: CancellationException) { }
            try { store.install("de", de, { false }) { }; fail() } catch (_: IllegalStateException) { }
            assertTrue(store.ready("de", de)); assertTrue(store.ready("fr", fr)); assertEquals("untouched", sentinel.readText())
        } finally { root.deleteRecursively() }
    }
}
