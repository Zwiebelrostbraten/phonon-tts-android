package ai.gradium.phonon.tts

/** Preference snapshot; selection changes only through an explicit installed-model choice. */
internal data class ModelSelection(val selected: String?, val defaults: Map<String, String>)
internal object SelectionPolicy {
    fun removed(installed: List<RouteModel>, id: String, current: ModelSelection, systemTags: List<String>): ModelSelection {
        val defaults = current.defaults.filterValues { it != id }
        val selected = current.selected?.takeIf { key -> key != id && installed.any { it.id == key } }
            ?: Routing.systemLanguage(systemTags, Routing.languages(installed.sortedBy { it.id }))?.let { Routing.choose(installed, it, defaults[it])?.id }
            ?: installed.sortedBy { it.id }.firstOrNull()?.id
        return ModelSelection(selected, defaults)
    }
    fun choose(installed: List<RouteModel>, id: String, current: ModelSelection): ModelSelection {
        val entry = installed.singleOrNull { it.id == id } ?: return current
        return ModelSelection(entry.id, current.defaults + entry.languages.associateWith { entry.id })
    }
}
