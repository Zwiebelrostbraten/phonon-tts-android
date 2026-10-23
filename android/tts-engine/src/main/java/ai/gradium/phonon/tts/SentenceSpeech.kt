package ai.gradium.phonon.tts

import java.text.BreakIterator
import java.util.Locale

/** Sentence estimates only: the native engine provides no word alignment. Offsets are UTF-16. */
object SentenceSpans {
    data class Span(val from: Int, val until: Int, val start: Int, val end: Int)

    fun split(text: String, locale: Locale): List<Span> {
        val result = ArrayList<Span>()
        val iterator = BreakIterator.getSentenceInstance(locale)
        // Paragraph breaks are natural boundaries even without terminal punctuation.
        var paragraph = 0
        while (paragraph < text.length) {
            val newline = text.indexOf('\n', paragraph)
            val limit = if (newline < 0) text.length else newline + 1
            val body = text.substring(paragraph, limit)
            iterator.setText(body)
            var from = iterator.first()
            var until = iterator.next()
            while (until != BreakIterator.DONE) {
                var start = paragraph + from
                var end = paragraph + until
                while (start < end && text[start].isWhitespace()) start++
                while (end > start && text[end - 1].isWhitespace()) end--
                if (start < end) result.add(Span(paragraph + from, paragraph + until, start, end))
                from = until
                until = iterator.next()
            }
            paragraph = limit
        }
        return result
    }
}

/** Runs on the synthesis thread, under the service's model lock. No audio is buffered here. */
class SentenceSpeech(
    private val rate: Int,
    private val maxBytes: Int,
    private val stopped: () -> Boolean,
    private val synthesize: (String, (FloatArray) -> Boolean) -> Unit,
    private val send: (ByteArray, Int) -> Boolean,
    private val range: (Int, Int, Int) -> Unit,
) {
    enum class Result { COMPLETE, STOPPED, OUTPUT_ERROR }

    fun speak(text: String, locale: Locale): Result {
        var frames = 0L
        for (span in SentenceSpans.split(text, locale)) {
            if (stopped()) return Result.STOPPED
            var marked = false
            val audio = SpeechAudio(rate, 100, maxBytes) { bytes, n ->
                if (stopped() || frames + n / 2 > Int.MAX_VALUE) false
                else {
                    // Android requires this before audioAvailable; a rejected block ends the request.
                    if (!marked) {
                        range(frames.toInt(), span.start, span.end)
                        marked = true
                    }
                    if (stopped() || !send(bytes, n)) false
                    else { frames += n / 2; true }
                }
            }
            if (!audio.accepting) return Result.OUTPUT_ERROR
            try {
                synthesize(text.substring(span.from, span.until)) { pcm ->
                    !stopped() && audio.accepting && audio.write(pcm)
                }
            } catch (e: Exception) {
                // Native synthesis may report its early callback exit as an exception.
                if (stopped()) return Result.STOPPED
                if (!audio.accepting) return Result.OUTPUT_ERROR
                throw e
            }
            if (stopped()) return Result.STOPPED
            if (!audio.finish()) return Result.OUTPUT_ERROR
        }
        return if (stopped()) Result.STOPPED else Result.COMPLETE
    }
}
