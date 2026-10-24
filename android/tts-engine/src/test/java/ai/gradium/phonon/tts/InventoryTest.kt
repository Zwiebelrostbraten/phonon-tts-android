package ai.gradium.phonon.tts
import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import java.io.File
class InventoryTest {
    @Test fun legacyAndIndependentRecovery() {
        val root = Files.createTempDirectory("inventory").toFile()
        fun checkpoint(dir: File) { dir.mkdirs(); dir.resolve("config.json").writeText("{}"); dir.resolve("tokenizer.json").writeText("{}"); dir.resolve("model.q8.gguf").writeText("sentinel") }
        try {
            checkpoint(root.resolve("model.backup"))
            checkpoint(root.resolve("models/custom-a"))
            checkpoint(root.resolve("model.staging"))
            val inventory = Inventory(root)
            assertEquals(setOf("legacy", "custom-a"), inventory.directories().keys)
            assertEquals("sentinel", root.resolve("model/model.q8.gguf").readText())
            assertEquals("sentinel", root.resolve("model.staging/model.q8.gguf").readText())
            val target = inventory.target("custom-b")
            checkpoint(File(target.parentFile, target.name + ".staging"))
            assertEquals(2, inventory.directories().size)
            try { inventory.target("../escape"); fail() } catch (_: IllegalArgumentException) {}
            val stage = File(target.parentFile, target.name + ".staging")
            try { CheckpointSwap.replace(stage,target) { error("activation failed") }; fail() } catch (_: IllegalStateException) {}
            assertFalse(target.exists())
            assertEquals("sentinel", root.resolve("models/custom-a/model.q8.gguf").readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun routingPreferencesAndVoiceIdentity() {
        val entries = listOf(RouteModel("multi", listOf("en","fr")), RouteModel("fr", listOf("fr")))
        assertEquals(listOf("en","fr"), Routing.languages(entries))
        assertEquals("de", Model.code("de-DE"))
        assertEquals("fr", Model.code("fr-CA"))
        assertEquals("pt", Model.code("pt-BR"))
        assertEquals("de", Model.code("ger"))
        assertNull(Model.code("ja-JP"))
        assertEquals("fr", Routing.choose(entries,"fr",null)?.id)
        assertEquals("multi", Routing.choose(entries,"fr","multi")?.id)
        assertNull(Routing.choose(entries,"de",null))
        assertEquals("fr", Routing.systemLanguage(listOf("ja-JP","fr-CA"), listOf("en","fr")))
        assertNotEquals(Routing.voice("a","en","default"), Routing.voice("b","en","default"))
    }
    @Test fun catalogComplete() {
        assertEquals(7, ModelCatalog.all.size)
        assertEquals(setOf("en","de","fr","es","pt"), ModelCatalog.all.flatMap { it.languages }.toSet())
        ModelCatalog.all.forEach { m -> assertEquals(4,m.files.size); assertTrue(m.revision.matches(Regex("[a-f0-9]{40}"))); m.files.forEach { assertTrue(it.size > 0); assertEquals(if(it.gitBlob) 40 else 64,it.hash.length) } }
    }
    @Test fun cancellationAndSameIdentityRollbackPreserveAllSentinels() {
        val root = Files.createTempDirectory("transactions").toFile()
        try {
            val inventory = Inventory(root)
            val first = inventory.target("first").apply { mkdirs(); resolve("weight").writeText("first sentinel") }
            val other = inventory.target("other").apply { mkdirs(); resolve("weight").writeText("other sentinel") }
            val staging = File(first.parentFile,"first.staging").apply { mkdirs(); resolve("weight").writeText("new candidate") }
            try { inventory.commit("first",staging,{ true }) { fail("cancel must not activate") }; fail() } catch (_: java.util.concurrent.CancellationException) {}
            assertEquals("first sentinel",first.resolve("weight").readText())
            try { inventory.commit("first",staging,{ false }) { error("real activation callback failed") }; fail() } catch (_: IllegalStateException) {}
            assertEquals("first sentinel",first.resolve("weight").readText())
            assertEquals("other sentinel",other.resolve("weight").readText())
        } finally { root.deleteRecursively() }
    }
    @Test fun aliasesOnlyResolveKnownVoicesAgainstLanguageDefault() {
        val a = VoiceIdentity("english31","en","default","en-default")
        val b = VoiceIdentity("english40","en","default","en-default")
        val voices = listOf(a,b)
        val preferred = mapOf("en" to "english40")
        assertEquals(b,Routing.resolve("en-default",voices,preferred))
        assertEquals(a,Routing.resolve(Routing.voice(a.id,a.lang,a.speaker),voices,preferred))
        assertNull(Routing.resolve("en-invented",voices,preferred))
        assertNull(Routing.resolve("de-default",voices,preferred))
        assertEquals("pt",Routing.systemLanguage(listOf("pt-BR"),listOf("en","pt")))
    }

    @Test fun cancellationDuringCommitRestoresPreviousDirectory() {
        val root = Files.createTempDirectory("cancel-commit").toFile()
        try {
            val inventory = Inventory(root)
            val target = inventory.target("first").apply { mkdirs(); resolve("weight").writeText("old sentinel") }
            val staging = File(target.parentFile,"first.staging").apply { mkdirs(); resolve("weight").writeText("candidate") }
            var cancelled = false
            try { inventory.commit("first",staging,{ cancelled }) { cancelled = true }; fail("late cancellation committed") }
            catch (_: java.util.concurrent.CancellationException) {}
            assertEquals("old sentinel",target.resolve("weight").readText())
        } finally { root.deleteRecursively() }
    }

    @Test fun failedInstallRestoresPreviousActivationAndOriginalError() {
        val root = Files.createTempDirectory("restore-activation").toFile()
        try {
            for (sameId in listOf(true,false)) {
                val inventory = Inventory(root)
                val previous = inventory.target("previous").apply { mkdirs(); resolve("weight").writeText("old") }
                val target = if (sameId) previous else inventory.target("candidate")
                val staging = File(target.parentFile,target.name + ".staging").apply { mkdirs(); resolve("weight").writeText("new") }
                var handles = 1
                val original = IllegalStateException("install failed")
                try {
                    handles-- // production closes the previous activation before commit
                    try {
                        inventory.commit(target.name,staging,{ false }) {
                            assertEquals(0,handles)
                            handles++
                            throw original
                        }
                    } catch (t: Throwable) {
                        CheckpointSwap.restoreActivation(t, { handles = 0 }, { CheckpointSwap.recover(target) }) {
                            assertEquals(0,handles)
                            assertEquals("old",previous.resolve("weight").readText())
                            handles++
                        }
                    }
                    fail("must preserve install failure")
                } catch (t: IllegalStateException) { assertSame(original,t) }
                assertEquals(1,handles)
            }
            val original = IllegalStateException("install failed")
            val recovery = IllegalStateException("recovery failed")
            try {
                CheckpointSwap.restoreActivation(original, {}, { throw recovery }, { fail("unsafe activation") })
            } catch (t: IllegalStateException) {
                assertSame(original,t)
                assertSame(recovery,t.suppressed.single())
            }
        } finally { root.deleteRecursively() }
    }

}
