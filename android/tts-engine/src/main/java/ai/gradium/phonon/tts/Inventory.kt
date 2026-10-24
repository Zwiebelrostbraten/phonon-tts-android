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
            CheckpointSwap.recover(dir)
            if (complete(dir)) id to dir else null
        }.toMap()
    }
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
