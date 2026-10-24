package ai.gradium.phonon.tts

import org.junit.Assert.*
import org.junit.Test
import java.io.File

class VoiceCatalogPolicyTest {
    private val catalog: List<BundledVoice> = run {
        val text = File("src/main/assets/voices-manifest.json").readText()
        Regex("\\{[^{}]+\\}").findAll(text).map { match ->
            fun value(key: String) = Regex("\"$key\": \\\"([^\\\"]+)\\\"").find(match.value)!!.groupValues[1]
            BundledVoice(value("name"), value("language"), value("filename"),
                Regex("\"bytes\": (\\d+)").find(match.value)!!.groupValues[1].toLong(), value("sha256"))
        }.toList()
    }
    @Test fun exactLanguageCountsAndCheckpointEligibility() {
        val expected = mapOf("en" to 37, "es" to 25, "de" to 13, "fr" to 15, "pt" to 12)
        assertEquals(102, catalog.size)
        assertEquals(expected, catalog.groupingBy { it.language }.eachCount())
        for ((language, count) in expected) {
            assertEquals(count, VoiceCatalogPolicy.forLanguages(catalog, listOf(language)).size)
            val filtered = VoiceCatalogPolicy.filter(catalog.map { it.stem }, catalog, language)
            assertEquals(count, filtered.size)
            assertTrue(filtered.all { stem -> catalog.single { it.stem == stem }.language == language })
        }
        assertEquals(102, VoiceCatalogPolicy.forLanguages(catalog, expected.keys.toList()).size)
        assertEquals(0, VoiceCatalogPolicy.forLanguages(catalog, listOf("ja")).size)
    }
    @Test fun frenchOnlyImportDoesNotAdvertiseKnownGermanCanonicalFile() {
        val german = catalog.first { it.language == "de" }
        val frenchEntries = VoiceCatalogPolicy.forLanguages(catalog, listOf("fr"))
        assertTrue(frenchEntries.none { it == german })
        val imported = listOf("default", "my-import", german.stem)
        assertEquals(listOf("default", "my-import"),
            VoiceCatalogPolicy.available(imported, catalog, "fr") { false })
    }
    @Test fun onlyAvailableFilesAndRelevantHandles() {
        val de = catalog.first { it.language == "de" }
        assertEquals(listOf("default", "import"), VoiceCatalogPolicy.available(listOf("default", "import"), catalog, "fr") { it == de })
        assertEquals(listOf("default", de.stem), VoiceCatalogPolicy.available(listOf("default"), catalog, "de") { it == de })
        assertTrue(VoiceCatalogPolicy.invalidates(listOf("en", "de"), "de"))
        assertFalse(VoiceCatalogPolicy.invalidates(listOf("fr"), "de"))
        assertFalse(VoiceCatalogPolicy.invalidates(emptyList(), "de"))
    }
    @Test fun livePublicLanguagePacksThroughProductionDownloader() {
        org.junit.Assume.assumeTrue(System.getenv("PHONON_LIVE_VOICES") == "1")
        val root = File(System.getenv("TMPDIR"), "phonon-live-packs").apply { mkdirs() }
        val store = VoicePackStore(root)
        for (language in listOf("en", "de", "fr", "es", "pt")) {
            val entries = catalog.filter { it.language == language }
            store.install(language, entries, { false }) { into ->
                SecureDownload({ false }) { _, _ -> }.downloadFiles(
                    "gradium/phonon-voices", "0ee17205f254b5abc43b266c8a0f215565c352f7",
                    entries.map { ModelFile(it.asset, it.bytes, it.sha256) }, null, into)
            }
            assertTrue(store.ready(language, entries))
            println("LIVE verified $language: ${entries.size} voices, ${entries.sumOf { it.bytes }} bytes")
        }
    }
    @Test fun germanIdentityAndImportedNamesRemainStable() {
        val nele = catalog.single { it.name == "Nele" }
        assertEquals("Gradium_Nele_5d5ad59fdd4642cd162f082873b27f658705a5d8598957d6274b30bf4bd4dc0b", nele.stem)
        val imported = listOf("default", "my-import", "Gradium_Nele_fake", "Gradium_Custom")
        assertEquals(imported, VoiceCatalogPolicy.filter(imported + nele.stem, catalog, "fr"))
        assertEquals(imported + nele.stem, VoiceCatalogPolicy.filter(imported + nele.stem, catalog, "de"))
        assertEquals(emptyList<String>(), VoiceCatalogPolicy.filter(emptyList(), catalog, "en"))
    }
}
