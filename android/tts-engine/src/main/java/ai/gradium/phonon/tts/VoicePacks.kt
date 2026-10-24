package ai.gradium.phonon.tts

import android.content.Context
import java.io.File

internal object VoicePacks {
    const val REPO = "gradium/phonon-voices"
    const val REVISION = "0ee17205f254b5abc43b266c8a0f215565c352f7"
    @Volatile var generation = 0; private set
    private val lock = Any()
    private fun store(context: Context) = VoicePackStore(File(context.filesDir, "voice-packs"))
    fun entries(context: Context, language: String) = BundledVoices.entries(context).filter { it.language == language }
    fun available(context: Context, entry: BundledVoice): File? {
        val dir = store(context).target(entry.language)
        // The service serializes replacement; restore a prior pack after process death.
        if (!InstallService.running && File(dir.parentFile, dir.name + ".backup").exists()) synchronized(lock) { CheckpointSwap.recover(dir) }
        if (!File(dir.parentFile, dir.name + ".backup").exists() && File(dir, "complete").isFile) {
            File(dir, entry.asset).takeIf { VoicePackStore.cachedVerified(it, entry) }?.let { return it }
        }
        // Existing installed German/catalog identities remain usable without a network request.
        for (model in Model.entries(context)) for (folder in listOf("voices", "embeddings")) {
            File(model.directory, "$folder/${entry.stem}.safetensors").takeIf { VoicePackStore.cachedVerified(it, entry) }?.let { return it }
        }
        return null
    }
    fun ready(context: Context, language: String) = entries(context, language).let { it.isNotEmpty() && it.all { v -> available(context, v) != null } }
    fun download(context: Context, language: String) = synchronized(lock) {
        val entries = entries(context, language)
        val client = SecureDownload({ InstallService.cancelled }) { done, total -> InstallService.packProgress(context, done, total) }
        InstallService.packDownloader(client)
        store(context).install(language, entries, { InstallService.cancelled }) { staging ->
            client.downloadFiles(REPO, REVISION, entries.map { ModelFile(it.asset, it.bytes, it.sha256) }, null, staging)
        }
        Model.voicesChanged(language)
        generation++
    }
}
