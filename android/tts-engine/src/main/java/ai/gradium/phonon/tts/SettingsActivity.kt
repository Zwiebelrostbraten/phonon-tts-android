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
import android.view.View
import android.graphics.drawable.GradientDrawable

/**
 * The engine's settings, which the system text-to-speech settings open: what is installed, and a
 * way to install a checkpoint folder from the phone's storage.
 */
class SettingsActivity : Activity() {
    private lateinit var installedInfo: TextView
    private lateinit var previewSection: LinearLayout
    private lateinit var cancelButton: Button
    private var sizeForModel: Model.Loaded? = null
    private var installedSize = 0L
    private lateinit var status: TextView
    private lateinit var importButton: Button
    private var importBytes = 0L

    private lateinit var downloadButton: Button
    private lateinit var token: android.widget.EditText
    private lateinit var models: android.widget.Spinner
    private var preview: android.speech.tts.TextToSpeech? = null
    private var previewReady = false
    private lateinit var voicePicker: android.widget.Spinner
    private var voiceChoices = emptyList<Pair<String, String>>()
    private var shownModel: Model.Loaded? = null
    private lateinit var tokenStatus: TextView
    private val tokenStore by lazy { TokenStore(applicationContext) }
    private fun updateTokenStatus() {
        tokenStatus.text = try {
            if (tokenStore.read() != null) getString(R.string.token_saved) else getString(R.string.token_none)
        } catch (_: Exception) { getString(R.string.token_error) }
    }
    private fun saveToken(): Boolean { return try {
        if (token.text.isBlank()) { updateTokenStatus(); return false }
        tokenStore.save(token.text.toString().trim())
        token.text.clear(); updateTokenStatus(); true
    } catch (_: Exception) { tokenStatus.text = getString(R.string.token_error); false }
    }
    private var pendingPreview: (() -> Unit)? = null
    private val handler = android.os.Handler(android.os.Looper.getMainLooper())
    private val poll = object : Runnable {
        override fun run() { refresh(); handler.postDelayed(this, 500) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pad = (16 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad) }
        var group = column
        fun add(view: View) { group.addView(view, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = pad / 2 }) }
        fun label(value: String) = TextView(this).apply { text = value; textSize = 16f; setPadding(0, pad / 2, 0, pad / 2); add(this) }
        fun button(title: Int, action: () -> Unit) = Button(this).apply { setText(title); isAllCaps = false; minHeight = 48 * pad / 16; setOnClickListener { action() }; add(this) }
        fun section(title: Int): LinearLayout {
            group = column
            val surface = android.util.TypedValue().also { theme.resolveAttribute(android.R.attr.colorBackground, it, true) }.data
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL; setPadding(pad, pad, pad, pad)
                background = GradientDrawable().apply { cornerRadius = pad.toFloat(); setColor(surface); setStroke(1, 0x33808080) }
            }
            column.addView(card, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT).apply { bottomMargin = pad })
            group = card
            label(getString(title)).apply { textSize = 20f; setTypeface(null, android.graphics.Typeface.BOLD); accessibilityHeadingCompat() }
            return card
        }
        label(getString(R.string.app_name)).apply { textSize = 28f; accessibilityHeadingCompat() }
        section(R.string.installed_model)
        installedInfo = label("")
        status = label("").apply { accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE }
        cancelButton = button(R.string.cancel) { InstallService.cancel(); pendingPreview = null; preview?.stop() }
        val downloadSection = section(R.string.model_actions)
        label(getString(R.string.replacement_help))
        val chooser = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; visibility = View.GONE }
        downloadButton = button(R.string.choose_download) { chooser.visibility = if (chooser.visibility == View.VISIBLE) View.GONE else View.VISIBLE }
        add(chooser)
        group = chooser
        models = android.widget.Spinner(this).apply {
            contentDescription = getString(R.string.choose_download)
            minimumHeight = 48 * pad / 16
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
        tokenStatus = label("")
        updateTokenStatus()
        val save = Button(this).apply { setText(R.string.save_token); setOnClickListener { saveToken() }; add(this) }
        val delete = Button(this).apply { setText(R.string.delete_token); setOnClickListener {
            try { tokenStore.delete(); token.text.clear(); updateTokenStatus() }
            catch (_: Exception) { tokenStatus.text = getString(R.string.token_error) }
        }; add(this) }
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
                tokenStatus.visibility = token.visibility; save.visibility = token.visibility; delete.visibility = token.visibility
                token.text.clear()
            }
        }
        button(R.string.download) {
            if (InstallService.running || Model.installing) return@button
            val model = ModelCatalog.all[models.selectedItemPosition]
            if (model.gated && token.text.isNotBlank() && !saveToken()) return@button
            val readToken = try { if (model.gated) tokenStore.read() else null }
                catch (_: Exception) { updateTokenStatus(); return@button }
            token.text.clear()
            val app = applicationContext
            confirmReplacement { InstallService.start(this) { InstallService.download(app, model, readToken) }; chooser.visibility = View.GONE }
            refresh()
        }
        group = downloadSection
        section(R.string.local_import)
        label(getString(R.string.import_help))
        importButton = button(R.string.import_folder) { confirmReplacement { startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE).addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION), PICK) } }
        previewSection = section(R.string.voice_preview)
        label(getString(R.string.voice))
        val voices = android.widget.Spinner(this).apply { contentDescription = getString(R.string.voice); minimumHeight = 48 * pad / 16; add(this) }
        voicePicker = voices
        voices.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                voiceChoices.getOrNull(position)?.let { (lang, speaker) -> BundledVoices.select(this@SettingsActivity, lang, speaker) }
            }
        }
        var rate = 100
        fun slider(title: Int, changed: (Int) -> Unit) {
            val value = label(getString(title, 100))
            add(android.widget.SeekBar(this).apply {
                contentDescription = getString(R.string.preview_rate, 100)
                minimumHeight = 48 * pad / 16
                max = 150; progress = 50
                setOnSeekBarChangeListener(object : android.widget.SeekBar.OnSeekBarChangeListener {
                    override fun onStartTrackingTouch(bar: android.widget.SeekBar?) {}
                    override fun onStopTrackingTouch(bar: android.widget.SeekBar?) {}
                    override fun onProgressChanged(bar: android.widget.SeekBar?, progress: Int, user: Boolean) {
                        changed(progress + 50); value.text = getString(title, progress + 50); bar?.contentDescription = value.text
                    }
                })
            })
        }
        slider(R.string.preview_rate) { rate = it }
        val sample = android.widget.EditText(this).apply { setText(getString(R.string.preview_text)); contentDescription = getString(R.string.sample_text); minHeight = 48 * pad / 16; add(this) }
        button(R.string.preview) {
            if (Model.state !is Model.State.Ready || Model.installing) {
                refresh()
                return@button
            }
            val requestedText = sample.text.toString()
            val requestedRate = rate / 100f
            val requestedVoice = voiceChoices.getOrNull(voices.selectedItemPosition)
            val speakRequested = {
                val engine = preview
                if (previewReady && engine != null && Model.state is Model.State.Ready && !Model.installing) {
                    // A model may have no optional voice embeddings; use its default in that case.
                    val choice = requestedVoice ?: voiceChoices.getOrNull(voices.selectedItemPosition)
                    if (choice != null) {
                        val voice = engine.voices?.firstOrNull { it.name == BundledVoices.androidName(choice.first, choice.second) }
                        if (voice != null) { engine.language = voice.locale; engine.voice = voice }
                    }
                    engine.setSpeechRate(requestedRate); engine.setPitch(1f)
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
                            if (!previewReady) {
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
        // Move preview directly after installed status.
        column.removeView(previewSection)
        column.addView(previewSection, 2)
        section(R.string.system_section)
        button(R.string.system_settings) { startActivity(Intent("com.android.settings.TTS_SETTINGS")) }
        section(R.string.details_section)
        label(getString(R.string.about_fork))
        button(R.string.licenses) {
            val names = listOf("LICENSE-MIT", "LICENSE-APACHE", "Sonic-LICENSE.txt", "NOTICE.txt", "German-Voices-NOTICE.txt")
            val text = names.joinToString("\n\n") { name -> name + "\n" + assets.open("licenses/$name").bufferedReader().use { it.readText() } }
            val view = TextView(this).apply { this.text = text; setPadding(pad, pad, pad, pad); setTextIsSelectable(true) }
            android.app.AlertDialog.Builder(this).setTitle(R.string.licenses)
                .setView(ScrollView(this).apply { addView(view) }).setPositiveButton(android.R.string.ok, null).show()
        }
        setContentView(ScrollView(this).apply {
            addView(column)
            setOnApplyWindowInsetsListener { view, insets ->
                if (android.os.Build.VERSION.SDK_INT >= 30) {
                    val bars = insets.getInsets(android.view.WindowInsets.Type.systemBars() or android.view.WindowInsets.Type.ime())
                    view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
                } else {
                    @Suppress("DEPRECATION")
                    view.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop, insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                }
                insets
            }
        })
        Model.load(this)
        refresh()
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

    private fun TextView.accessibilityHeadingCompat() {
        if (android.os.Build.VERSION.SDK_INT >= 28) isAccessibilityHeading = true
    }

    private fun confirmReplacement(action: () -> Unit) {
        if (InstallService.running || Model.installing) return
        if (!Model.dir(this).exists()) { action(); return }
        android.app.AlertDialog.Builder(this).setTitle(R.string.replace_title)
            .setMessage(R.string.replace_message)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(R.string.replace_confirm) { _, _ ->
                if (!InstallService.running && !Model.installing) action()
            }.show()
    }

    private fun refresh() {
        val busy = InstallService.running || Model.installing
        val loaded = (Model.state as? Model.State.Ready)?.model
        if (loaded !== shownModel) {
            pendingPreview = null; preview?.stop(); preview?.shutdown(); preview = null; previewReady = false
            shownModel = loaded
            voiceChoices = loaded?.languages?.flatMap { lang ->
                BundledVoices.speakers(loaded, lang).ifEmpty { listOf("default") }.map { lang to it }
            }.orEmpty()
            val selected = loaded?.let { model -> voiceChoices.indexOfFirst { (lang, speaker) ->
                speaker == BundledVoices.selected(this, model, lang)
            } } ?: -1
            voicePicker.adapter = android.widget.ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item,
                voiceChoices.map { (lang, speaker) -> "${BundledVoices.label(speaker)} ($lang)" })
            if (selected >= 0) voicePicker.setSelection(selected)
        }
        previewSection.visibility = if (loaded != null && !busy) View.VISIBLE else View.GONE
        cancelButton.visibility = if (busy) View.VISIBLE else View.GONE
        if (loaded !== sizeForModel || installedSize == 0L) {
            sizeForModel = loaded
            installedSize = Model.dir(this).walkTopDown().filter { it.isFile }.sumOf { it.length() }
        }
        installedInfo.text = if (Model.dir(this).exists()) getString(R.string.installed_details,
            (loaded?.languages ?: Model.languages(this)).joinToString(", "), installedSize / 1_000_000.0)
            else getString(R.string.not_installed)
        voicePicker.isEnabled = loaded != null && !busy
        importButton.isEnabled = !busy
        downloadButton.isEnabled = !busy
        models.isEnabled = !busy
        val modelStatus = when (val s = Model.state) {
            is Model.State.NotInstalled -> getString(R.string.not_installed)
            is Model.State.Loading -> getString(R.string.loading)
            is Model.State.Failed -> getString(R.string.load_failed, s.message)
            is Model.State.Ready -> getString(R.string.ready, s.model.languages.joinToString(", "), s.model.speakers.joinToString(", ") { BundledVoices.label(it) }.ifEmpty { "-" })
        }
        status.text = listOf(modelStatus, InstallService.message).filter { it.isNotBlank() }.joinToString("\n")
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
