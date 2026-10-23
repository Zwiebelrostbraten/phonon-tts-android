package ai.gradium.phonon.tts

import android.content.Context
import android.system.Os
import java.io.FileOutputStream
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest

object BundledVoices {
    private fun digest(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
    /** Only called before constructing a native handle, under the model lifecycle. Never replaces a user file. */
    fun install(context: Context, checkpoint: File, languages: List<String>) {
        if ("de" !in languages) return
        val entries = JSONObject(context.assets.open("german-voices/manifest.json").bufferedReader().use { it.readText() }).getJSONArray("voices")
        val voices = File(checkpoint, "voices").apply { check(isDirectory || mkdirs()) }
        for (i in 0 until entries.length()) {
            val entry = entries.getJSONObject(i)
            val bytes = context.assets.open("german-voices/${entry.getString("name")}.safetensors").use { it.readBytes() }
            val hash = entry.getString("sha256")
            check(bytes.size.toLong() == entry.getLong("bytes") && digest(bytes) == hash) { "Invalid bundled voice" }
            // Full content hash provides a stable namespace, even with imported voices of the same name.
            val stem = "Gradium_${entry.getString("name")}_$hash"
            val target = File(voices, "$stem.safetensors")
            val embedding = File(checkpoint, "embeddings/$stem.safetensors")
            if (target.exists()) { check(digest(target.readBytes()) == hash) { "Bundled voice name collision; preserve existing file" }; continue }
            if (embedding.exists()) { check(digest(embedding.readBytes()) == hash); continue }
            // A .part is invisible to the native loader, including after process death on API 24.
            val temporary = File(voices, ".${target.name}.part")
            try {
                FileOutputStream(temporary).use { output -> output.write(bytes); output.fd.sync() }
                check(!target.exists()) { "Voice appeared during installation" }
                Os.rename(temporary.path, target.path)
            } finally { temporary.delete() }
        }
    }
    fun label(speaker: String): String = if (speaker.startsWith("Gradium_")) speaker.removePrefix("Gradium_").substringBefore('_') else speaker
    fun androidName(lang: String, speaker: String) = "$lang-${if (speaker.startsWith("Gradium_")) label(speaker) + " (Gradium)" else speaker}"
    fun speakers(model: Model.Loaded, lang: String) = model.speakers.filter { lang == "de" || !it.startsWith("Gradium_") }
    fun selected(context: Context, model: Model.Loaded, lang: String): String? {
        val available = speakers(model, lang)
        return context.getSharedPreferences("voices", Context.MODE_PRIVATE).getString(lang, null)?.takeIf { it in available }
            ?: model.defaultSpeaker?.takeIf { it in available } ?: available.firstOrNull()
    }
    fun select(context: Context, lang: String, speaker: String) {
        context.getSharedPreferences("voices", Context.MODE_PRIVATE).edit().putString(lang, speaker).apply()
    }
}
