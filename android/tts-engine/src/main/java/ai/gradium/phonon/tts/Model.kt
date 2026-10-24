package ai.gradium.phonon.tts

import ai.gradium.phonon.PhononTTS
import android.content.Context
import android.system.Os
import org.json.JSONObject
import java.io.File
import java.util.Locale
import java.util.concurrent.Executors
import java.util.concurrent.Future

object Model {
    val SUPPORTED = listOf("en", "fr", "de", "es", "pt")
    /** Every native operation, including synthesis and close, owns this lock. Never lock a handle. */
    val lifecycle = Any()
    class Loaded(val tts: PhononTTS, val speakers: List<String>, val languages: List<String>, val id: String) {
        val defaultSpeaker = if ("default" in speakers) "default" else speakers.firstOrNull()
    }
    data class Entry(val id: String, val directory: File, val languages: List<String>, val catalog: DownloadModel?)
    sealed class State {
        object NotInstalled : State()
        object Loading : State()
        class Ready(val model: Loaded) : State()
        class Failed(val message: String) : State()
    }
    @Volatile var state: State = State.NotInstalled; private set
    @Volatile var installing = false; private set
    private val installLock = Any()
    private val loader = Executors.newSingleThreadExecutor()
    private val metadataLock = Any()
    @Volatile private var snapshot: List<Entry>? = null
    fun entries(context: Context): List<Entry> = snapshot ?: synchronized(metadataLock) {
        snapshot ?: discover(context).also { snapshot = it }
    }
    private fun discover(context: Context): List<Entry> =
        Inventory(context.filesDir).directories().map { (id, dir) ->
            val catalog = ModelCatalog.all.firstOrNull { m ->
                val expected = m.files.first { it.name == "config.json" }
                val bytes = File(dir,"config.json").readBytes()
                DownloadPolicy.hex(DownloadPolicy.digest(expected).digest(bytes)) == expected.hash &&
                    File(dir,"model.q8.gguf").length() == m.files.first { it.name == "model.q8.gguf" }.size
            }
            Entry(id,dir,catalog?.languages ?: checkpointLanguages(dir),catalog)
        }
    private fun prefs(context: Context) = context.getSharedPreferences("models",Context.MODE_PRIVATE)
    fun selected(context: Context): Entry? {
        val entries = entries(context)
        return entries.firstOrNull { it.id == prefs(context).getString("selected",null) } ?: route(context,systemLanguage(context),entries) ?: entries.firstOrNull()
    }
    fun select(context: Context, id: String) {
        val preferences = prefs(context)
        val current = ModelSelection(preferences.getString("selected", null),
            preferences.all.mapNotNull { (key, value) ->
                if (key.startsWith("language-") && value is String) key.removePrefix("language-") to value else null
            }.toMap())
        val next = SelectionPolicy.choose(entries(context).map { RouteModel(it.id, it.languages) }, id, current)
        if (next === current) return // Unknown installed identity: no writes and no native load.
        preferences.edit().apply {
            putString("selected", next.selected)
            next.defaults.forEach { (language, model) -> putString("language-$language", model) }
        }.apply()
        load(context)
    }
    fun preferredLanguages(context: Context, id: String) = SUPPORTED.filter { prefs(context).getString("language-$it",null) == id }
    fun route(context: Context, lang: String?, entries: List<Entry> = entries(context)): Entry? {
        if(lang == null) return null
        val chosen = Routing.choose(entries.map { RouteModel(it.id,it.languages) },lang,prefs(context).getString("language-$lang",null))
        return entries.firstOrNull { it.id == chosen?.id }
    }
    fun systemLanguage(context: Context): String? {
        val locales = context.resources.configuration.locales
        return Routing.systemLanguage((0 until locales.size()).map { locales[it].toLanguageTag() }, languages(context))
    }
    fun dir(context: Context) = selected(context)?.directory ?: File(context.filesDir,"model")
    fun installed(context: Context) = entries(context).isNotEmpty()
    fun languages(context: Context) = Routing.languages(entries(context).map { RouteModel(it.id,it.languages) })
    private fun checkpointLanguages(checkpoint: File): List<String> {
        val listed = runCatching { JSONObject(File(checkpoint,"config.json").readText()).optJSONArray("languages") }.getOrNull()
        val names = (0 until (listed?.length() ?: 0)).mapNotNull { code(listed!!.optString(it)) }
        // Unknown legacy imports must not advertise all five training languages.
        return if (listed == null || listed.length() == 0) listOf("en") else names.distinct()
    }
    fun load(context: Context): Future<*> = loader.submit { synchronized(lifecycle) { selected(context)?.let { activate(context,it) } } }
    fun await(context: Context): Loaded? = synchronized(lifecycle) { selected(context)?.let { activate(context,it) } }
    fun <T> use(context: Context, entry: Entry, work: (Loaded) -> T): T? = synchronized(lifecycle) { activate(context,entry)?.let(work) }
    fun voicesChanged(language: String) = synchronized(lifecycle) {
        if (VoiceCatalogPolicy.invalidates((state as? State.Ready)?.model?.languages.orEmpty(), language)) close()
    }
    private fun close() { (state as? State.Ready)?.model?.tts?.close(); state = State.NotInstalled }
    private fun activate(context: Context, entry: Entry): Loaded? {
        if (!Inventory(context.filesDir).live(entry.id, entry.directory)) return null
        (state as? State.Ready)?.model?.takeIf { it.id == entry.id }?.let { return it }
        close(); state = State.Loading
        return try {
            Os.setenv("RAYON_NUM_THREADS","2",false)
            BundledVoices.install(context,entry.directory,entry.languages)
            val tts = PhononTTS(entry.directory.path,entry.languages.firstOrNull() ?: "none")
            val loaded = try { Loaded(tts,tts.voices,entry.languages,entry.id) } catch(t: Throwable) { tts.close(); throw t }
            state = State.Ready(loaded); loaded
        } catch(t: Throwable) { state = State.Failed(context.getString(R.string.activation_error)); null }
    }
    fun install(context: Context, cancelled: () -> Boolean = { false }, id: String = "custom-" + java.util.UUID.randomUUID(), fill: (File) -> Unit) = synchronized(installLock) {
        entries(context) // establish a recovery snapshot before creating transaction markers
        installing = true
        val target = Inventory(context.filesDir).target(id)
        val staging = File(target.parentFile,target.name + ".staging")
        try {
            synchronized(lifecycle) {
                val marker = File(target.parentFile, target.name + ".deleted")
                if (marker.exists()) {
                    // A previous failed deletion is never an installation backup.
                    for (suffix in listOf("", ".backup", ".staging")) {
                        val old = File(target.parentFile, target.name + suffix)
                        check(!old.exists() || old.deleteRecursively())
                    }
                    check(marker.deleteRecursively())
                }
                CheckpointSwap.recover(target)
            }
            check(!staging.exists() || staging.deleteRecursively())
            check(staging.mkdirs())
            fill(staging)
            require(Inventory.complete(staging))
            if(cancelled()) throw java.util.concurrent.CancellationException()
            synchronized(lifecycle) {
                val previousEntries = entries(context)
                val previous = (state as? State.Ready)?.model?.id?.let { activeId -> previousEntries.firstOrNull { it.id == activeId } }
                val previousSelection = prefs(context).getString("selected",null)
                close() // validation must never create a second native handle
                try {
                    Inventory(context.filesDir).commit(id,staging,cancelled) {
                        if(cancelled()) throw java.util.concurrent.CancellationException()
                        check(activate(context,Entry(id,target,ModelCatalog.all.firstOrNull { it.id == id }?.languages ?: checkpointLanguages(target),null)) != null) { context.getString(R.string.activation_error) }
                        if(cancelled()) { close(); throw java.util.concurrent.CancellationException() }
                    }
                    snapshot = discover(context)
                    prefs(context).edit().putString("selected",id).apply()
                } catch(t: Throwable) {
                    CheckpointSwap.restoreActivation(t, { close() }, { CheckpointSwap.recover(target) }) {
                        snapshot = previousEntries
                        prefs(context).edit().putString("selected",previousSelection).apply()
                        previous?.takeIf {
                            Inventory.complete(it.directory) && !File(it.directory.parentFile,it.directory.name + ".backup").exists()
                        }?.let { check(activate(context,it) != null) { context.getString(R.string.activation_error) } }
                    }
                }
            }
        } finally { staging.deleteRecursively(); installing = false }
    }
    fun delete(context: Context, id: String) = synchronized(installLock) {
        installing = true
        try { synchronized(lifecycle) {
            val inventory = Inventory(context.filesDir)
            require(entries(context).any { it.id == id } && inventory.live(id, inventory.target(id)))
            if ((state as? State.Ready)?.model?.id == id) close()
            try { inventory.remove(id) } finally {
                snapshot = discover(context)
                if (snapshot!!.isEmpty() && state !is State.Ready) state = State.NotInstalled
                if (snapshot!!.none { it.id == id }) {
                    val preferences = prefs(context)
                    val current = ModelSelection(preferences.getString("selected", null), preferences.all.mapNotNull { (key, value) ->
                        if (key.startsWith("language-") && value is String) key.removePrefix("language-") to value else null
                    }.toMap())
                    val locales = context.resources.configuration.locales
                    val next = SelectionPolicy.removed(snapshot!!.map { RouteModel(it.id,it.languages) }, id, current,
                        (0 until locales.size()).map { locales[it].toLanguageTag() })
                    preferences.edit().apply {
                        if (next.selected == null) remove("selected") else putString("selected", next.selected)
                        current.defaults.filterValues { it == id }.keys.forEach { remove("language-$it") }
                    }.apply()
                }
            }
        } } finally { installing = false }
    }
    fun code(language: String?): String? {
        if(language.isNullOrEmpty()) return null
        val value = language.lowercase(Locale.ROOT).replace('_','-')
        val aliases = mapOf("fre" to "fr","fra" to "fr","ger" to "de","deu" to "de","eng" to "en","spa" to "es","por" to "pt")
        return aliases[value] ?: Locale.forLanguageTag(value).language.takeIf { it in SUPPORTED }
    }
}
