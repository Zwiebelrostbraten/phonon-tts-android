package ai.gradium.phonon.tts

/** Preference snapshot; selection changes only through an explicit installed-model choice. */
internal data class ModelSelection(val selected: String?, val defaults: Map<String, String>)
internal object SelectionPolicy {
    fun choose(installed: List<RouteModel>, id: String, current: ModelSelection): ModelSelection {
        val entry = installed.singleOrNull { it.id == id } ?: return current
        return ModelSelection(entry.id, current.defaults + entry.languages.associateWith { entry.id })
    }
}
