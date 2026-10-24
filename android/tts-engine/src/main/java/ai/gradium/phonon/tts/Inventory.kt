package ai.gradium.phonon.tts

import java.io.File

/** Storage discovery never moves a legacy checkpoint. All transactions have a private sibling backup. */
class Inventory(private val root: File) {
    fun target(id: String): File {
        require(id.matches(Regex("[a-zA-Z0-9_-]{1,100}")))
        return if (id == "legacy") File(root,"model") else File(root,"models/$id")
    }
    fun directories(): Map<String, File> {
        val ids = linkedSetOf("legacy")
        File(root,"models").listFiles()?.forEach { file ->
            val id = file.name.removeSuffix(".backup").removeSuffix(".staging")
            if (id.matches(Regex("[a-zA-Z0-9_-]{1,100}"))) ids += id
        }
        return ids.mapNotNull { id ->
            val dir = target(id)
            if (File(dir.parentFile, dir.name + ".deleted").exists()) return@mapNotNull null
            CheckpointSwap.recover(dir)
            if (complete(dir)) id to dir else null
        }.toMap()
    }
    /** Identity only; never accepts a UI path. Marker survives failures and prevents recovery. */
    fun remove(id: String) {
        val dir = target(id)
        require(live(id, dir)) { "Model is no longer installed" }
        val anchor = root.canonicalFile
        for (suffix in listOf("", ".backup", ".staging")) {
            val source = File(dir.parentFile, dir.name + suffix)
            val expected = File(anchor, source.relativeTo(root).path)
            require(source.canonicalFile == expected) { "Unsafe model path" }
            require(source.walkTopDown().all { it.canonicalFile == File(expected, it.relativeTo(source).path) }) { "Unsafe model contents" }
        }
        val marker = File(dir.parentFile, dir.name + ".deleted")
        check(marker.mkdir()) { "Cannot mark model for deletion" }
        // Mark first: a crash at any later point must never resurrect a backup.
        for (suffix in listOf(".backup", ".staging", "")) {
            val source = File(dir.parentFile, dir.name + suffix)
            if (source.exists()) check(source.renameTo(File(marker, "checkpoint" + suffix))) { "Cannot quarantine model" }
        }
        check(marker.deleteRecursively()) { "Model removed from inventory, but storage cleanup failed" }
    }
    fun live(id: String, directory: File): Boolean =
        target(id) == directory && !File(directory.parentFile, directory.name + ".deleted").exists() && complete(directory)

    fun <T> commit(id: String, staging: File, cancelled: () -> Boolean, activate: () -> T): T {
        if(cancelled()) throw java.util.concurrent.CancellationException()
        return CheckpointSwap.replace(staging,target(id)) {
            val value = activate()
            if(cancelled()) throw java.util.concurrent.CancellationException()
            value
        }
    }
    companion object {
        fun complete(dir: File) = File(dir,"config.json").isFile && File(dir,"tokenizer.json").isFile &&
            (File(dir,"model.q8.gguf").isFile || File(dir,"model.safetensors").isFile)
    }
}
data class RouteModel(val id: String, val languages: List<String>)
data class VoiceIdentity(val id: String, val lang: String, val speaker: String, val alias: String)
object Routing {
    fun languages(models: List<RouteModel>) = models.flatMap { it.languages }.distinct()
    fun resolve(name: String?, voices: List<VoiceIdentity>, preferred: Map<String,String>): VoiceIdentity? =
        voices.firstOrNull { voice(it.id,it.lang,it.speaker) == name } ?: voices.firstOrNull {
            preferred[it.lang] == it.id && (it.alias == name || "${it.lang}-${it.speaker}" == name)
        }
    fun choose(models: List<RouteModel>, lang: String, preferred: String?): RouteModel? =
        models.filter { lang in it.languages }.sortedWith(compareBy<RouteModel> { if(it.id == preferred) 0 else 1 }.thenBy { it.languages.size }.thenBy { it.id }).firstOrNull()
    fun systemLanguage(tags: List<String>, installed: List<String>): String? =
        tags.map { java.util.Locale.forLanguageTag(it).language }.firstOrNull { it in installed } ?: installed.firstOrNull()
    fun voice(id: String, lang: String, speaker: String) = "$id:$lang:$speaker"
}
