package ai.gradium.phonon.tts

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.nio.file.Files

class ModelDeletionTest {
    private fun checkpoint(dir: File) = dir.apply {
        mkdirs()
        resolve("config.json").writeText("{}")
        resolve("tokenizer.json").writeText("{}")
        resolve("model.q8.gguf").writeText("weights:$name")
    }
    private fun fixture(work: (File, Inventory) -> Unit) {
        val root = Files.createTempDirectory("deletion").toFile()
        try { work(root, Inventory(root)) } finally { root.deleteRecursively() }
    }
    @Test fun cancelDoesNotInvokeRemovalOrChangePreferences() = fixture { _, inventory ->
        val dir = checkpoint(inventory.target("a"))
        val prefs = ModelSelection("a", mapOf("en" to "a"))
        val gate = ReplacementGate()
        gate.request(true, false) { inventory.remove("a") }
        gate.cancel()
        assertTrue(dir.exists()); assertEquals("a", prefs.selected)
        assertEquals(mapOf("en" to "a"), prefs.defaults)
    }
    @Test fun deletingOnePreservesOtherSharedPacksAndTokenAndRejectsStalePath() = fixture { root, inventory ->
        val a = checkpoint(inventory.target("a"))
        val b = checkpoint(inventory.target("b"))
        val sentinels = listOf(b.resolve("model.q8.gguf"), root.resolve("voice-packs/en/pack"), root.resolve("token-prefs"))
        sentinels.drop(1).forEach { it.parentFile!!.mkdirs(); it.writeText("untouched") }
        val before = sentinels.map { it.readBytes().toList() }
        inventory.remove("a")
        assertEquals(setOf("b"), inventory.directories().keys)
        assertFalse(inventory.live("a", a))
        assertEquals(before, sentinels.map { it.readBytes().toList() })
        assertFalse(inventory.live("b", a))
    }
    @Test fun legacyWithBackupAndStagingNeverReturnsAfterRestart() = fixture { root, inventory ->
        checkpoint(inventory.target("legacy"))
        checkpoint(root.resolve("model.backup")); checkpoint(root.resolve("model.staging"))
        inventory.remove("legacy")
        assertTrue(Inventory(root).directories().isEmpty())
        assertFalse(root.resolve("model.backup").exists()); assertFalse(root.resolve("model.staging").exists())
    }
    @Test fun lastModelClearsOnlyReferencingDefaults() = fixture { _, inventory ->
        checkpoint(inventory.target("a")); inventory.remove("a")
        val next = SelectionPolicy.removed(emptyList(), "a", ModelSelection("a", mapOf("en" to "a")), listOf("en"))
        assertTrue(inventory.directories().isEmpty()); assertNull(next.selected); assertTrue(next.defaults.isEmpty())
    }
    @Test fun activeFallbackUsesLocaleAndPreservesUnrelatedDefaults() {
        val remaining = listOf(RouteModel("en", listOf("en")), RouteModel("fr", listOf("fr")))
        val current = ModelSelection("a", mapOf("en" to "a", "fr" to "fr"))
        val next = SelectionPolicy.removed(remaining,"a",current,listOf("fr-CA"))
        assertEquals("fr", next.selected); assertEquals(mapOf("fr" to "fr"), next.defaults)
        assertEquals("en", SelectionPolicy.removed(remaining,"a",current.copy(selected="en"),listOf("fr")).selected)
    }
    @Test fun invalidOrUninstalledIdentityMakesNoWrites() = fixture { root, inventory ->
        val a = checkpoint(inventory.target("a"))
        for (id in listOf("../a", "", "/", "a.backup", "unknown")) {
            try { inventory.remove(id); fail(id) } catch (_: IllegalArgumentException) { }
        }
        assertEquals(setOf("models"), root.list()!!.toSet()); assertTrue(a.exists())
        assertEquals(setOf("a"), inventory.directories().keys)
    }
    @Test fun symlinkCannotDeleteOutsideInstallation() = fixture { root, inventory ->
        val dir = checkpoint(inventory.target("a"))
        val shared = root.resolve("voice-packs").apply { mkdirs(); resolve("sentinel").writeText("keep") }
        Files.createSymbolicLink(dir.resolve("escape").toPath(), shared.toPath())
        try { inventory.remove("a"); fail("symlink accepted") } catch (_: IllegalArgumentException) { }
        assertEquals("keep", shared.resolve("sentinel").readText())
        assertTrue(inventory.live("a", dir))
        dir.resolve("escape").delete()
    }
    @Test fun legitimateRootAncestorSymlinkAllowsDeletion() = fixture { root, _ ->
        val actual = root.resolve("actual/files").apply { mkdirs() }
        val alias = root.resolve("alias")
        Files.createSymbolicLink(alias.toPath(), actual.parentFile!!.toPath())
        val inventory = Inventory(alias.resolve("files"))
        for (id in listOf("legacy", "a")) {
            checkpoint(inventory.target(id))
            checkpoint(File(inventory.target(id).path + ".backup"))
            checkpoint(File(inventory.target(id).path + ".staging"))
            inventory.remove(id)
            assertFalse(inventory.target(id).exists())
        }
        assertTrue(Inventory(actual).directories().isEmpty())
        alias.delete()
    }
    @Test fun aliasedRootStillRejectsSymlinkedModelPaths() = fixture { root, _ ->
        val actual = root.resolve("files").apply { mkdirs() }
        val outside = root.resolve("files-sibling").apply { mkdirs() }
        val alias = root.resolve("alias")
        Files.createSymbolicLink(alias.toPath(), actual.toPath())
        val inventory = Inventory(alias)
        for (relative in listOf("model", "models")) {
            val link = actual.resolve(relative)
            val target = if (relative == "model") checkpoint(outside.resolve("legacy"))
                else outside.apply { checkpoint(resolve("a")) }
            Files.createSymbolicLink(link.toPath(), target.toPath())
            try { inventory.remove(if (relative == "model") "legacy" else "a"); fail("symlink accepted") }
            catch (_: IllegalArgumentException) { }
            assertTrue(Inventory.complete(if (relative == "model") target else target.resolve("a")))
            assertFalse(File(inventory.target(if (relative == "model") "legacy" else "a").path + ".deleted").exists())
            link.delete()
        }
        alias.delete()
    }
    @Test fun interruptedDeletionMarkerBlocksBackupRecovery() = fixture { root, inventory ->
        checkpoint(root.resolve("model.backup")); root.resolve("model.deleted").mkdir()
        assertTrue(inventory.directories().isEmpty()); assertFalse(root.resolve("model").exists())
    }
}
