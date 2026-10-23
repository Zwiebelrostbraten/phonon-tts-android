package ai.gradium.phonon.tts

import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.Locale

class ProgressTest {
    @Test fun serviceSuppliesPlaybackRangeMarkers() {
        val source = File("src/main/java/ai/gradium/phonon/tts/PhononService.kt").readText()
        assertTrue("Service never supplies playback range markers", source.contains("callback.rangeStart("))
    }

    @Test fun markersFollowActualTempoOutputAndOriginalOffsets() {
        val text = "  😀 Hallo Welt! Noch ein Satz.\nOhne Punkt"
        for (rate in listOf(50, 100, 200)) {
            var frames = 0
            val markers = ArrayList<Triple<Int, Int, Int>>()
            val spoken = StringBuilder()
            var sentenceNumber = 0
            val expected = SentenceSpans.split(text, Locale.GERMAN)
            val result = SentenceSpeech(rate, 255, { false }, { sentence, pcm ->
                sentenceNumber++
                spoken.append(sentence)
                repeat(8) { assertTrue(pcm(FloatArray(997) { 0.2f })) }
            }, { _, n ->
                assertTrue(n <= 255 && n % 2 == 0)
                assertEquals("Marker must precede the first block", sentenceNumber, markers.size)
                frames += n / 2
                true
            }, { frame, start, end ->
                assertEquals(frames, frame)
                markers.add(Triple(frame, start, end))
            }).speak(text, Locale.GERMAN)
            assertEquals(SentenceSpeech.Result.COMPLETE, result)
            assertEquals(text, spoken.toString())
            assertEquals(expected.size, markers.size)
            assertEquals(0, markers.first().first)
            assertEquals(2, markers.first().second)
            assertEquals(expected.map { it.start to it.end }, markers.map { it.second to it.third })
            assertTrue(markers.zipWithNext().all { it.first.first < it.second.first })
            assertEquals(7976.0 * expected.size * 100 / rate, frames.toDouble(), 1000.0)
        }
    }

    @Test fun segmentationPreservesUnicodeParagraphsAndNumbers() {
        for (text in listOf("", "  \n ", "  Ein Satz ohne Punkt 😀  ",
            "„Grüße, Herr Müller!“\nDer Wert ist 3.14 Euro. Weiter geht’s!", "Dr. Müller z. B. liest. Danach schläft er.")) {
            val spans = SentenceSpans.split(text, Locale.GERMAN)
            val covered = BooleanArray(text.length)
            for (span in spans) {
                assertTrue(span.start < span.end)
                assertFalse(Character.isLowSurrogate(text[span.start]))
                assertFalse(Character.isHighSurrogate(text[span.end - 1]))
                for (i in span.from until span.until) { assertFalse(covered[i]); covered[i] = true }
                assertFalse(text.substring(span.start, span.end).isBlank())
            }
            assertTrue(text.indices.all { covered[it] || text[it].isWhitespace() })
            if (text.contains("3.14")) assertTrue(spans.any { text.substring(it.from, it.until).contains("3.14") })
        }
        assertEquals(1, SentenceSpans.split("Ohne Punkt 😀", Locale.GERMAN).size)
    }

    @Test fun stopAndSinkFailureEndRequestWithoutFutureSpans() {
        for (cancel in listOf(false, true)) {
            var stopped = false
            var synthesized = 0
            var blocks = 0
            var markers = 0
            val result = SentenceSpeech(100, 8, { stopped }, { _, pcm ->
                synthesized++
                assertFalse(pcm(FloatArray(100)))
                assertFalse(pcm(FloatArray(100)))
                throw IllegalStateException("Native callback exit")
            }, { _, _ -> blocks++; stopped = cancel; false }, { _, _, _ -> markers++ })
                .speak("First sentence. Second sentence.", Locale.ENGLISH)
            assertEquals(if (cancel) SentenceSpeech.Result.STOPPED else SentenceSpeech.Result.OUTPUT_ERROR, result)
            assertEquals(1, synthesized)
            assertEquals(1, blocks)
            assertEquals(1, markers)
        }
    }

    @Test fun emptyAudioInvalidBufferAndPreexistingStop() {
        var markers = 0
        var synths = 0
        fun runner(max: Int, stop: Boolean) = SentenceSpeech(200, max, { stop }, { _, _ -> synths++ },
            { _, _ -> fail("No audio expected"); false }, { _, _, _ -> markers++ })
        assertEquals(SentenceSpeech.Result.COMPLETE, runner(8, false).speak(" \n ", Locale.GERMAN))
        assertEquals(SentenceSpeech.Result.COMPLETE, runner(8, false).speak("Silent.", Locale.GERMAN))
        assertEquals(SentenceSpeech.Result.OUTPUT_ERROR, runner(1, false).speak("Text.", Locale.GERMAN))
        assertEquals(SentenceSpeech.Result.STOPPED, runner(8, true).speak("Text.", Locale.GERMAN))
        assertEquals(0, markers)
        assertEquals(1, synths)
    }
}
