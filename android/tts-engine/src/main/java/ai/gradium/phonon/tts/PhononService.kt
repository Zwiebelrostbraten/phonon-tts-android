package ai.gradium.phonon.tts

import android.media.AudioFormat
import android.speech.tts.SynthesisCallback
import android.speech.tts.SynthesisRequest
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeechService
import android.speech.tts.Voice
import android.util.Log
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Phonon as a system text-to-speech engine. Android calls [onSynthesizeText] on one thread, one
 * request at a time, and [onStop] from another to cut the current one short.
 *
 * Android voices are named `<model-id>:<language>:<speaker>`: every checkpoint voice in every language the
 * checkpoint speaks.
 */
class PhononService : TextToSpeechService() {
    @Volatile private var stopped = false
    @Volatile private var language: String? = null

    override fun onCreate() {
        super.onCreate()
        language = Model.systemLanguage(this)
    }

    // Answered from the installed files, without waiting for the model to load.
    override fun onIsLanguageAvailable(lang: String?, country: String?, variant: String?): Int {
        val code = Model.code(lang) ?: return TextToSpeech.LANG_NOT_SUPPORTED
        // The checkpoints have no regional variants, so a country never matches more closely.
        return if (code in Model.languages(this)) TextToSpeech.LANG_AVAILABLE else TextToSpeech.LANG_MISSING_DATA
    }

    override fun onLoadLanguage(lang: String?, country: String?, variant: String?): Int {
        val result = onIsLanguageAvailable(lang, country, variant)
        if (result >= TextToSpeech.LANG_AVAILABLE) language = Model.code(lang)
        return result
    }

    override fun onGetLanguage(): Array<String> {
        val code = language ?: Model.languages(this).firstOrNull() ?: return arrayOf("", "", "")
        return arrayOf(Locale.forLanguageTag(code).isO3Language, "", "")
    }

    override fun onGetVoices(): List<Voice> {
        return Model.entries(this).flatMap { entry -> entry.languages.flatMap { lang ->
            BundledVoices.metadata(this, entry, lang).map { speaker -> voice(entry.id,lang,speaker) }
        } }
    }
    override fun onIsValidVoiceName(voiceName: String?): Int = if(parse(voiceName) != null) TextToSpeech.SUCCESS else TextToSpeech.ERROR
    override fun onLoadVoice(voiceName: String?): Int {
        val choice = parse(voiceName) ?: return TextToSpeech.ERROR
        language = choice.lang
        return TextToSpeech.SUCCESS
    }
    override fun onGetDefaultVoiceNameFor(lang: String?, country: String?, variant: String?): String? {
        val code = Model.code(lang) ?: return null
        val entry = Model.route(this,code) ?: return null
        return Routing.voice(entry.id,code,BundledVoices.selectedMetadata(this,entry,code))
    }

    override fun onStop() {
        stopped = true
    }

    override fun onSynthesizeText(request: SynthesisRequest, callback: SynthesisCallback) {
        stopped = false
        val choice = parse(request.voiceName) ?: Model.code(request.language)?.let { lang ->
            Model.route(this,lang)?.let { entry -> Choice(entry,lang,BundledVoices.selectedMetadata(this,entry,lang)) }
        } ?: run { fail(callback,TextToSpeech.ERROR_INVALID_REQUEST); return }
        val lang = choice.lang
        val speaker = choice.speaker
        val text = request.charSequenceText?.toString().orEmpty()
        if (callback.start(SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT, 1) != TextToSpeech.SUCCESS) return
        if (text.isBlank()) {
            callback.done()
            return
        }
        try {
            val result = Model.use(this,choice.entry) { model ->
                model.tts.setLanguage(lang)
                if (speaker in model.speakers) model.tts.setVoice(speaker)
                SentenceSpeech(
                    request.speechRate, callback.maxBufferSize, { stopped },
                    { sentence, pcm -> model.tts.speak(sentence, pcm) },
                    { bytes, n -> callback.audioAvailable(bytes, 0, n) == TextToSpeech.SUCCESS },
                    { frame, start, end ->
                        if (android.os.Build.VERSION.SDK_INT >= 26) callback.rangeStart(frame, start, end)
                    },
                ).speak(text, Locale.forLanguageTag(lang))
            }
            when (result) {
                SentenceSpeech.Result.COMPLETE -> if (!stopped) callback.done()
                SentenceSpeech.Result.OUTPUT_ERROR -> if (!stopped) fail(callback, TextToSpeech.ERROR_OUTPUT)
                SentenceSpeech.Result.STOPPED -> Unit
                null -> if (!stopped) fail(callback, TextToSpeech.ERROR_SYNTHESIS)
            }
        } catch (e: Exception) {
            Log.e(TAG, "synthesis failed", e)
            if (!stopped) fail(callback, TextToSpeech.ERROR_SYNTHESIS)
        }
    }

    /** Android only tells the app about an error once the request is done. */
    private fun fail(callback: SynthesisCallback, code: Int) {
        callback.error(code)
        callback.done()
    }

    private fun voice(id: String, lang: String, speaker: String) = Voice(
        Routing.voice(id,lang,speaker), Locale.forLanguageTag(lang), Voice.QUALITY_HIGH, Voice.LATENCY_LOW, false, emptySet())
    private data class Choice(val entry: Model.Entry, val lang: String, val speaker: String)
    private fun parse(name: String?): Choice? {
        if(name == null) return null
        val entries = Model.entries(this)
        val voices = entries.flatMap { entry -> entry.languages.flatMap { lang ->
            BundledVoices.metadata(this,entry,lang).map { VoiceIdentity(entry.id,lang,it,BundledVoices.androidName(lang,it)) }
        } }
        val preferred = Model.SUPPORTED.mapNotNull { lang -> Model.route(this,lang,entries)?.let { lang to it.id } }.toMap()
        val found = Routing.resolve(name,voices,preferred) ?: return null
        return Choice(entries.first { it.id == found.id },found.lang,found.speaker)
    }

    private companion object {
        const val TAG = "PhononService"

        /** What ptts.h hands out: 24 kHz mono. */
        const val SAMPLE_RATE = 24000

        /** The voice name's speaker part for a checkpoint without voices. */
        const val NO_SPEAKER = "default"

        /** Floats in [-1, 1] to little-endian 16-bit PCM. */
        fun pcm16(pcm: FloatArray): ByteArray {
            val out = ByteArray(pcm.size * 2)
            for (i in pcm.indices) {
                val s = (pcm[i].coerceIn(-1f, 1f) * 32767f).roundToInt()
                out[2 * i] = s.toByte()
                out[2 * i + 1] = (s shr 8).toByte()
            }
            return out
        }
    }
}
