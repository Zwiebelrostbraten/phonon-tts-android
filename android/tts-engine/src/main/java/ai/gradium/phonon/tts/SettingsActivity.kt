package ai.gradium.phonon.tts

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import java.io.File

/** Platform work stays here; Compose receives display state and explicit user actions only. */
class SettingsActivity : ComponentActivity() {
    private var ui by mutableStateOf(SettingsUiState())
    private val replacement = ReplacementGate()
    private var preview: TextToSpeech? = null
    private var previewReady = false
    private var previewGeneration = 0
    private var localError: String? = null
    private var pendingPreview: (() -> Unit)? = null
    private var shownModel: Model.Entry? = null
    private var importBytes = 0L
    private var initializedSelection = false
    private var resumed = false
    private val tokenStore by lazy { TokenStore(applicationContext) }
    private val handler = Handler(Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() { if (resumed) { refresh(); handler.postDelayed(this, 500) } }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        ui = ui.copy(sample = getString(R.string.preview_text))
        updateTokenStatus()
        setContent {
            SettingsScreen(ui, SettingsActions(
                deleteInstalled = { id ->
                    if (!busy()) {
                        val app = applicationContext
                        localError = null
                        stopPreview()
                        InstallService.start(app, R.string.delete_model_complete, deletingModel = true) { Model.delete(app, id) }
                        refresh()
                    }
                },
                selectModel = { ui = ui.copy(selectedModel = it, token = "") },
                downloadPack = {
                    ui.voices.getOrNull(ui.selectedVoice)?.first?.let { language ->
                        InstallService.start(this, R.string.voice_pack_available) { VoicePacks.download(applicationContext, language) }
                        refresh()
                    }
                },
                selectInstalled = { Model.select(this,it); refresh() },
                selectVoice = { index -> ui.voices.getOrNull(index)?.let { BundledVoices.select(this, it.first, it.second) }; ui = ui.copy(selectedVoice = index, sample = Samples.text[ui.voices.getOrNull(index)?.first] ?: ui.sample) },
                editSample = { ui = ui.copy(sample = it) },
                changeRate = { ui = ui.copy(rate = it) },
                editToken = { ui = ui.copy(token = it) },
                saveToken = { saveToken() }, deleteToken = {
                    try { tokenStore.delete(); ui = ui.copy(token = ""); updateTokenStatus() }
                    catch (_: Exception) { ui = ui.copy(tokenStatus = getString(R.string.token_error)) }
                },
                download = { download() },
                importModel = { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), PICK) },
                cancelInstall = { InstallService.cancel(); stopPreview(); refresh() },
                preview = { speak() }, stopPreview = { stopPreview() },
                terms = { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ModelCatalog.all[ui.selectedModel].page))) },
                systemSettings = { startActivity(Intent("com.android.settings.TTS_SETTINGS")) },
                confirmReplacement = { replacement.accept(busy()); ui = ui.copy(confirmReplacement = false); refresh() },
                dismissReplacement = { replacement.cancel(); ui = ui.copy(confirmReplacement = false) },
                licenses = { ui = ui.copy(licenseText = listOf("LICENSE-MIT", "LICENSE-APACHE", "Sonic-LICENSE.txt", "NOTICE.txt", "Voices-NOTICE.txt").joinToString("\n\n") { name -> name + "\n" + assets.open("licenses/$name").bufferedReader().use { it.readText() } }) },
                dismissLicenses = { ui = ui.copy(licenseText = null) }
            ))
        }
        refresh()
    }
    private fun busy() = InstallService.running || Model.installing
    private fun updateTokenStatus() {
        ui = ui.copy(tokenStatus = try { getString(if (tokenStore.read() != null) R.string.token_saved else R.string.token_none) }
            catch (_: Exception) { getString(R.string.token_error) })
    }
    private fun saveToken(): Boolean {
        if (ui.token.isBlank()) { updateTokenStatus(); return false }
        return try { tokenStore.save(ui.token.trim()); ui = ui.copy(token = ""); updateTokenStatus(); true }
        catch (_: Exception) { ui = ui.copy(tokenStatus = getString(R.string.token_error)); false }
    }
    private fun download() {
        if (busy()) return
        localError = null
        val model = ModelCatalog.all[ui.selectedModel]
        // Saving credentials is always a separate explicit action.
        val token = try { if (model.gated) tokenStore.read() else null }
            catch (_: Exception) { updateTokenStatus(); return }
        confirmReplacement {
            InstallService.start(this) { InstallService.download(applicationContext, model, token, Model.entries(this).firstOrNull { it.catalog?.id == model.id }?.id ?: model.id) }
            refresh()
        }
    }
    private fun confirmReplacement(action: () -> Unit) {
        replacement.request(Model.entries(this).any { it.id == ModelCatalog.all[ui.selectedModel].id || it.catalog?.id == ModelCatalog.all[ui.selectedModel].id }, busy(), action)
        ui = ui.copy(confirmReplacement = replacement.pending)
    }
    private fun stopPreview() {
        pendingPreview = null; preview?.stop(); ui = ui.copy(speaking = false)
    }
    private fun speak() {
        if (shownModel == null || busy()) return
        val requestedText = ui.sample
        val requestedRate = ui.rate / 100f
        val requestedVoice = ui.voices.getOrNull(ui.selectedVoice)
        val requestedModel = shownModel
        val request: () -> Unit = {
            val engine = preview
            if (previewReady && engine != null && requestedModel === shownModel && !busy()) {
                requestedVoice?.let { choice -> engine.voices?.firstOrNull { it.name == Routing.voice(requestedModel?.id ?: "",choice.first, choice.second) }?.let { engine.language = it.locale; engine.voice = it } }
                engine.setSpeechRate(requestedRate)
                if (engine.speak(requestedText, TextToSpeech.QUEUE_FLUSH, null, "preview") == TextToSpeech.SUCCESS) ui = ui.copy(speaking = true)
            }
        }
        if (preview == null) {
            val generation = ++previewGeneration
            ui = ui.copy(previewError = "")
            pendingPreview = request
            preview = TextToSpeech(this, { result -> handler.post {
                if (generation == previewGeneration && resumed && !isDestroyed) {
                    previewReady = result == TextToSpeech.SUCCESS
                    if (!previewReady) { preview?.shutdown(); preview = null; ui = ui.copy(previewError = getString(R.string.preview_failed)) }
                    val pending = pendingPreview; pendingPreview = null
                    if (previewReady) pending?.invoke()
                }
            } }, packageName)
        } else if (previewReady) request() else pendingPreview = request
    }
    private var shownVoiceGeneration = -1
    private fun refresh() {
        val inventory = Model.entries(this)
        val selectedEntry = Model.selected(this)
        val state = Model.state
        if (!initializedSelection) {
            val current = selectedEntry?.catalog?.id
            val localeList = resources.configuration.locales
            val systemLang = Routing.systemLanguage((0 until localeList.size()).map { localeList[it].toLanguageTag() },Model.SUPPORTED)
            val recommended = ModelCatalog.all.firstOrNull { it.languages == listOf(systemLang) }?.id
            ui = ui.copy(selectedModel = initialModelSelection(ModelCatalog.all.map { it.id == (current ?: recommended) }))
            initializedSelection = true
        }
        if (selectedEntry !== shownModel || shownVoiceGeneration != VoicePacks.generation) {
            shownVoiceGeneration = VoicePacks.generation
            stopPreview(); previewGeneration++; preview?.shutdown(); preview = null; previewReady = false
            shownModel = selectedEntry
            val voices = selectedEntry?.let { entry -> entry.languages.flatMap { lang ->
                BundledVoices.metadata(this,entry,lang).map { lang to it }
            } }.orEmpty()
            val lang = ui.voices.getOrNull(ui.selectedVoice)?.first?.takeIf { it in selectedEntry?.languages.orEmpty() }
                ?: Model.systemLanguage(this)?.takeIf { it in selectedEntry?.languages.orEmpty() }
                ?: selectedEntry?.languages?.firstOrNull()
            val selected = selectedEntry?.let { entry -> voices.indexOfFirst { (code, speaker) ->
                code == lang && speaker == BundledVoices.selectedMetadata(this,entry,code)
            } }?.coerceAtLeast(0) ?: 0
            ui = ui.copy(voices = voices, selectedVoice = selected,
                sample = Samples.text[voices.getOrNull(selected)?.first] ?: ui.sample)
        }
        val status = when (state) {
            Model.State.NotInstalled -> getString(if(inventory.isEmpty()) R.string.not_installed else R.string.model_ready)
            Model.State.Loading -> getString(R.string.loading)
            is Model.State.Failed -> getString(R.string.load_failed, state.message)
            is Model.State.Ready -> getString(R.string.model_ready)
        }
        val installed = inventory.map { entry ->
            val label = entry.catalog?.let { getString(it.label) }
                ?: if(entry.id == "legacy") getString(R.string.legacy_model) else getString(R.string.custom_model,entry.id)
            val size = entry.directory.walkTopDown().filter { it.isFile }.sumOf { it.length() }
            val defaults = Model.preferredLanguages(this,entry.id)
            val details = getString(R.string.installed_details,entry.languages.joinToString(", "),size / 1_000_000.0) +
                if(defaults.isEmpty()) "" else "\n" + getString(R.string.language_defaults,defaults.joinToString(", "))
            InstalledChoice(entry.id,label,details)
        }
        val packLanguage = ui.voices.getOrNull(ui.selectedVoice)?.first
        val packEntries = packLanguage?.let { VoicePacks.entries(this, it) }.orEmpty()
        ui = ui.copy(packReady = packLanguage?.let { VoicePacks.ready(this, it) } ?: false,
            packLabel = if (packEntries.isEmpty()) "" else getString(R.string.download_voice_pack,
                java.util.Locale.forLanguageTag(packLanguage!!).getDisplayLanguage(resources.configuration.locales[0]),
                packEntries.size, packEntries.sumOf { it.bytes } / 1_000_000.0), installed = installed, selectedInstalled = selectedEntry?.id,
            installedCatalog = inventory.mapNotNull { it.catalog?.id }.toSet(),
            deleting = InstallService.deleting, ready = selectedEntry != null && !busy(), busy = busy(), loading = state is Model.State.Loading,
            status = if (busy()) getString(if (InstallService.deleting) R.string.deleting_model else R.string.installing_model) else status, error = state is Model.State.Failed,
            operation = localError ?: InstallService.message,
            speaking = ui.speaking && preview?.isSpeaking == true)
    }
    override fun onResume() { super.onResume(); resumed = true; handler.post(poll) }
    override fun onPause() { resumed = false; handler.removeCallbacksAndMessages(null); stopPreview(); previewGeneration++; preview?.shutdown(); preview = null; previewReady = false; ui = ui.copy(token = ""); super.onPause() }
    override fun onDestroy() { resumed = false; handler.removeCallbacksAndMessages(null); replacement.cancel(); pendingPreview = null; preview?.stop(); preview?.shutdown(); super.onDestroy() }
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val tree = data?.data
        if (requestCode != PICK || resultCode != RESULT_OK || tree == null) return
        val app = applicationContext
        try {
            localError = null
            contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val started = InstallService.start(this) {
                try { Model.install(app, { InstallService.cancelled }) { into -> copyCheckpoint(tree, into); InstallService.checkCancelled() } }
                finally { app.contentResolver.releasePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
            if (!started) contentResolver.releasePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) { localError = getString(R.string.operation_failed) }
        refresh()
    }
    /** Copy the files a checkpoint folder needs from the picked folder into [into]. */
    private fun copyCheckpoint(tree: Uri, into: File) {
        importBytes = 0L
        val root = children(tree, DocumentsContract.getTreeDocumentId(tree))
        // The library loads the GGUF when both are there, so the other is not worth the space.
        val skip = if ("model.q8.gguf" in root) "model.safetensors" else null
        for ((name, entry) in root) {
            val (id, isDir) = entry
            when {
                !isDir && name in FILES && name != skip -> copy(tree, id, File(into, name))
                isDir && name in VOICE_DIRS -> for ((voice, child) in children(tree, id)) {
                    // The name comes from the document provider: keep it inside the voices folder.
                    if (!child.second && voice.endsWith(".safetensors") && '/' !in voice && '\\' !in voice) {
                        copy(tree, child.first, File(into, "$name/$voice"))
                    }
                }
            }
        }
    }

    /** A folder's entries by name: each one's document id and whether it is a folder. */
    private fun children(tree: Uri, id: String): Map<String, Pair<String, Boolean>> {
        val uri = DocumentsContract.buildChildDocumentsUriUsingTree(tree, id)
        val columns = arrayOf(Document.COLUMN_DOCUMENT_ID, Document.COLUMN_DISPLAY_NAME, Document.COLUMN_MIME_TYPE)
        val out = mutableMapOf<String, Pair<String, Boolean>>()
        contentResolver.query(uri, columns, null, null, null)?.use {
            while (it.moveToNext()) out[it.getString(1)] = it.getString(0) to (it.getString(2) == Document.MIME_TYPE_DIR)
        }
        return out
    }

    private fun copy(tree: Uri, id: String, to: File) {
        InstallService.checkCancelled()
        to.parentFile?.mkdirs()
        val uri = DocumentsContract.buildDocumentUriUsingTree(tree, id)
        val input = contentResolver.openInputStream(uri) ?: error("cannot read ${to.name}")
        input.use { from -> java.io.FileOutputStream(to).use { output ->
            val buffer = ByteArray(64 * 1024)
            var total = 0L
            val limit = when (to.name) { "config.json", "tokenizer.json" -> 4L * 1024 * 1024; else -> if (to.parentFile?.name in VOICE_DIRS || to.name == "default-voice.safetensors") 16L * 1024 * 1024 else 512L * 1024 * 1024 }
            while (true) {
                InstallService.checkCancelled()
                val n = from.read(buffer)
                if (n == -1) break
                total += n
                importBytes += n
                require(total <= limit && importBytes <= 1024L * 1024 * 1024) { "Import exceeds 1 GiB limit" }
                require(to.usableSpace > n + 8L * 1024 * 1024) { "Not enough storage" }
                output.write(buffer, 0, n)
            }
            output.fd.sync()
        } }
    }

    private companion object {
        const val PICK = 1

        /** What a checkpoint folder holds, as android/README.md lists it. */
        val FILES = setOf("config.json", "tokenizer.json", "model.q8.gguf", "model.safetensors", "default-voice.safetensors")
        val VOICE_DIRS = setOf("voices", "embeddings")
    }
}
