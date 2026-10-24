package ai.gradium.phonon.tts

internal data class BundledVoice(val name: String, val language: String, val asset: String, val bytes: Long, val sha256: String) {
    val stem: String get() = "Gradium_${name}_$sha256"
}

internal object VoiceCatalogPolicy {
    fun invalidates(loadedLanguages: List<String>, installedLanguage: String) = installedLanguage in loadedLanguages
    fun available(speakers: List<String>, catalog: List<BundledVoice>, language: String, present: (BundledVoice) -> Boolean): List<String> =
        filter((speakers + catalog.filter { it.language == language && present(it) }.map { it.stem }).distinct(), catalog, language)
    fun forLanguages(catalog: List<BundledVoice>, languages: List<String>) = catalog.filter { it.language in languages }
    /** Only exact catalog identities have a catalog language. Imported names remain available. */
    fun filter(speakers: List<String>, catalog: List<BundledVoice>, language: String): List<String> {
        val languages = catalog.associate { it.stem to it.language }
        return speakers.filter { languages[it]?.let { lang -> lang == language } ?: true }
    }
}
