package ai.gradium.phonon.tts

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import java.io.File
import kotlin.concurrent.thread

/**
 * The engine's settings, which the system text-to-speech settings open: what is installed, and a
 * way to install a checkpoint folder from the phone's storage.
 */
class SettingsActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var importButton: Button
    private var importBytes = 0L

    private lateinit var downloadButton: Button
    private lateinit var token: android.widget.EditText
    private lateinit var models: android.widget.Spinner
    private var preview: android.speech.tts.TextToSpeech? = null
    private var previewReady = false
    private var pendingPreview: (() -> Unit)? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 500) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        fun add(view: android.view.View) { column.addView(view, MATCH_PARENT, WRAP_CONTENT) }
        fun label(value: String) = TextView(this).apply { text = value; setPadding(0, pad, 0, pad / 2); add(this) }
        fun button(title: Int, action: () -> Unit) = Button(this).apply { setText(title); setOnClickListener { action() }; add(this) }
        label(getString(R.string.app_name)).textSize = 24f
        status = label("")
        models = android.widget.Spinner(this).apply {
            adapter = android.widget.ArrayAdapter(this@SettingsActivity, android.R.layout.simple_spinner_dropdown_item,
                ModelCatalog.all.map { getString(it.label) })
            add(this)
        }
        val details = label("")
        token = android.widget.EditText(this).apply {
            hint = getString(R.string.token_hint)
            inputType = android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD
            isSingleLine = true; isSaveEnabled = false
            if (android.os.Build.VERSION.SDK_INT >= 26) importantForAutofill = android.view.View.IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
            add(this)
        }
        val terms = button(R.string.open_terms) {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(ModelCatalog.all[models.selectedItemPosition].page)))
        }
        models.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val model = ModelCatalog.all[position]
                details.text = getString(R.string.model_details, model.total / 1_000_000.0,
                    if (model.gated) "CC-BY-NC 4.0" else "Apache 2.0") + "\n" + getString(if (model.gated) R.string.gated_help else R.string.free_help)
                token.visibility = if (model.gated) android.view.View.VISIBLE else android.view.View.GONE
                terms.visibility = token.visibility
                token.text.clear()
            }
        }
        downloadButton = button(R.string.download) {
            val model = ModelCatalog.all[models.selectedItemPosition]
            val readToken = token.text.toString().trim().takeIf { model.gated && it.isNotEmpty() }
            token.text.clear()
            val app = applicationContext
            InstallService.start(this) { InstallService.download(app, model, readToken) }
            refresh()
        }
        button(R.string.cancel) { InstallService.cancel(); pendingPreview = null; preview?.stop() }
        importButton = button(R.string.import_folder) { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), PICK) }
        button(R.string.system_settings) { startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
        label(getString(R.string.preview_help))
        val voices = android.widget.Spinner(this).apply { add(this) }
        var availableVoices = emptyList<android.speech.tts.Voice>()
        var rate = 100
        var pitch = 100
        fun slider(title: Int, changed: (Int) -> Unit) {
            val value = label(getString(title, 100))
            add(android.widget.SeekBar(this).apply {
                max = 150; progress = 50
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(bar: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(bar: android.widget.SeekBar?) {}
                    override fun onProgressChanged(bar: android.widget.SeekBar?, progress: Int, user: Boolean) {
                        changed(progress + 50); value.text = getString(title, progress + 50)
                    }
                })
            })
        }
        slider(R.string.preview_rate) { rate = it }; slider(R.string.preview_pitch) { pitch = it }
        val sample = android.widget.EditText(this).apply { setText(getString(R.string.preview_text)); add(this) }
        button(R.string.preview) {
            if (Model.state !is Model.State.Ready || Model.installing) {
                refresh()
                return@button
            }
            val requestedText = sample.text.toString()
            val requestedRate = rate / 100f
            val requestedPitch = pitch / 100f
            val requestedVoice = availableVoices.getOrNull(voices.selectedItemPosition)
            val speakRequested = {
                val engine = preview
                if (previewReady && engine != null && Model.state is Model.State.Ready && !Model.installing) {
                    // A model may have no optional voice embeddings; use its default in that case.
                    val voice = requestedVoice ?: availableVoices.getOrNull(voices.selectedItemPosition)
                    if (voice != null) { engine.language = voice.locale; engine.voice = voice }
                    engine.setSpeechRate(requestedRate); engine.setPitch(requestedPitch)
                    engine.speak(requestedText, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, "preview")
                }
                Unit
            }
            if (preview == null) {
                pendingPreview = speakRequested
                preview = android.speech.tts.TextToSpeech(this, { result ->
                    // Always enqueue: even an immediate callback must run after preview is assigned.
                    handler.post {
                        if (!isFinishing && !isDestroyed) {
                            previewReady = result == android.speech.tts.TextToSpeech.SUCCESS
                            if (previewReady) {
                                availableVoices = preview?.voices?.filter { !it.isNetworkConnectionRequired }?.sortedBy { it.name }.orEmpty()
                                voices.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, availableVoices.map { it.name })
                                status.text = getString(R.string.preview_ready)
                            } else {
                                preview?.shutdown(); preview = null
                            }
                            val requested = pendingPreview
                            pendingPreview = null
                            if (previewReady) requested?.invoke()
                        }
                    }
                }, packageName)
            } else if (previewReady) {
                speakRequested()
            } else {
                pendingPreview = speakRequested
            }
        }
        button(R.string.stop_preview) { pendingPreview = null; preview?.stop() }
        label(getString(R.string.import_help))
        label(getString(R.string.about_fork))
        button(R.string.licenses) {
            val names = listOf("LICENSE-MIT", "LICENSE-APACHE", "Sonic-LICENSE.txt", "NOTICE.txt")
            val text = names.joinToString("\n\n") { name -> name + "\n" + assets.open("licenses/$name").bufferedReader().use { it.readText() } }
            val view = TextView(this).apply { this.text = text; setPadding(pad, pad, pad, pad); setTextIsSelectable(true) }
            android.app.AlertDialog.Builder(this).setTitle(R.string.licenses)
                .setView(ScrollView(this).apply { addView(view) }).setPositiveButton(android.R.string.ok, null).show()
        }
        setContentView(ScrollView(this).apply { fitsSystemWindows = true; addView(column) })
        Model.load(this)
    }

    override fun onResume() { super.onResume(); handler.post(poll) }
    override fun onPause() { handler.removeCallbacks(poll); pendingPreview = null; token.text.clear(); super.onPause() }
    override fun onDestroy() { pendingPreview = null; preview?.stop(); preview?.shutdown(); super.onDestroy() }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val tree = data?.data
        if (requestCode != PICK || resultCode != RESULT_OK || tree == null) return
        val app = applicationContext
        try {
            contentResolver.takePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            val started = InstallService.start(this) {
                try { Model.install(app, { InstallService.cancelled }) { into -> copyCheckpoint(tree, into); InstallService.checkCancelled() } }
                finally { app.contentResolver.releasePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION) }
            }
            if (!started) contentResolver.releasePersistableUriPermission(tree, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (e: Exception) { status.text = getString(R.string.import_failed, e.message ?: e.javaClass.simpleName) }
        refresh()
    }

    private fun refresh() {
        val busy = InstallService.running || Model.installing
        importButton.isEnabled = !busy
        downloadButton.isEnabled = !busy
        models.isEnabled = !busy
        status.text = if (InstallService.running || InstallService.message.isNotEmpty()) InstallService.message else when (val s = Model.state) {
            is Model.State.NotInstalled -> getString(R.string.not_installed)
            is Model.State.Loading -> getString(R.string.loading)
            is Model.State.Failed -> getString(R.string.load_failed, s.message)
            is Model.State.Ready -> getString(R.string.ready, s.model.languages.joinToString(", "), s.model.speakers.joinToString(", ").ifEmpty { "-" })
        }
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
