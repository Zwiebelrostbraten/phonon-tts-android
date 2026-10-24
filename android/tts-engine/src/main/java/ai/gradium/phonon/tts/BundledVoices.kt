package ai.gradium.phonon.tts

import android.content.Context
import android.system.Os
import java.io.FileOutputStream
import org.json.JSONObject
import java.io.File

object BundledVoices {
    @Volatile private var catalog: List<BundledVoice>? = null
    internal fun entries(context: Context): List<BundledVoice> = catalog ?: synchronized(this) {
        catalog ?: run {
            val array = JSONObject(context.assets.open("voices-manifest.json").bufferedReader().use { it.readText() }).getJSONArray("voices")
            List(array.length()) { i -> val v = array.getJSONObject(i)
                BundledVoice(v.getString("name"), v.getString("language"), v.getString("filename"), v.getLong("bytes"), v.getString("sha256"))
            }.also { catalog = it }
        }
    }
    /** Only called before constructing a native handle, under the model lifecycle. Never replaces a user file. */
    fun install(context: Context, checkpoint: File, languages: List<String>) {
        val entries = VoiceCatalogPolicy.forLanguages(entries(context), languages)
        val voices = File(checkpoint, "voices").apply { check(isDirectory || mkdirs()) }
        for (entry in entries) {
            val target = File(voices, "${entry.stem}.safetensors")
            if (VoicePackStore.existingCanonical(checkpoint, entry)) continue
            val source = VoicePacks.available(context, entry) ?: continue
            val temporary = File(voices, ".${target.name}.part")
            try {
                FileOutputStream(temporary).use { output -> source.inputStream().use { it.copyTo(output) }; output.fd.sync() }
                check(VoicePackStore.verified(temporary, entry))
                check(!VoicePackStore.existingCanonical(checkpoint, entry)) { "Voice appeared during installation" }
                Os.rename(temporary.path, target.path)
            } finally { temporary.delete() }
        }
    }
    /** File names and bundled manifest only; Android voice enumeration never loads weights. */
    fun metadata(context: Context, entry: Model.Entry, lang: String): List<String> {
        val names = linkedSetOf<String>()
        if(File(entry.directory,"default-voice.safetensors").isFile) names += "default"
        for(folder in listOf("voices","embeddings")) File(entry.directory,folder).listFiles()?.filter { it.isFile && it.name.endsWith(".safetensors") }?.sortedBy { it.name }?.forEach { names += it.name.removeSuffix(".safetensors") }
        val catalog = entries(context)
        names.removeAll { name -> catalog.firstOrNull { it.stem == name }?.let { voice ->
            listOf("voices", "embeddings").none { folder -> VoicePackStore.cachedVerified(File(entry.directory, "$folder/$name.safetensors"), voice) }
        } ?: false }
        return VoiceCatalogPolicy.available(names.toList(), catalog, lang) { VoicePacks.available(context, it) != null }
    }

    fun selectedMetadata(context: Context, entry: Model.Entry, lang: String): String {
        val available = metadata(context,entry,lang)
        return context.getSharedPreferences("voices",Context.MODE_PRIVATE).getString(lang,null)?.takeIf { it in available } ?: available.firstOrNull { it == "default" } ?: available.firstOrNull() ?: "default"
    }
    fun label(speaker: String): String = if (speaker.startsWith("Gradium_")) speaker.removePrefix("Gradium_").substringBefore('_') else speaker
    fun androidName(lang: String, speaker: String) = "$lang-${if (speaker.startsWith("Gradium_")) label(speaker) + " (Gradium)" else speaker}"
    fun speakers(context: Context, model: Model.Loaded, lang: String) = VoiceCatalogPolicy.filter(model.speakers, entries(context), lang)
    fun selected(context: Context, model: Model.Loaded, lang: String): String? {
        val available = speakers(context, model, lang)
        return context.getSharedPreferences("voices", Context.MODE_PRIVATE).getString(lang, null)?.takeIf { it in available }
            ?: model.defaultSpeaker?.takeIf { it in available } ?: available.firstOrNull()
    }
    fun select(context: Context, lang: String, speaker: String) {
        context.getSharedPreferences("voices", Context.MODE_PRIVATE).edit().putString(lang, speaker).apply()
    }
}
