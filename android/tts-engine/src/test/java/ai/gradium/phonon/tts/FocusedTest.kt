package ai.gradium.phonon.tts

import org.junit.Assert.*
import org.junit.Test
import java.nio.file.Files
import kotlin.math.PI
import kotlin.math.sin

class FocusedTest {
    @Test fun tokenEncryptionRoundTripAndTamperRejection() {
        val key = javax.crypto.KeyGenerator.getInstance("AES").apply { init(256) }.generateKey()
        val encrypted = TokenEnvelope.seal("synthetic-test-token", key)
        assertEquals("synthetic-test-token", TokenEnvelope.open(encrypted, key))
        assertFalse(String(encrypted, Charsets.ISO_8859_1).contains("synthetic-test-token"))
        assertFalse(encrypted.contentEquals(TokenEnvelope.seal("synthetic-test-token", key)))
        encrypted[encrypted.lastIndex] = (encrypted.last().toInt() xor 1).toByte()
        try { TokenEnvelope.open(encrypted, key); fail("Tampering accepted") } catch (_: javax.crypto.AEADBadTagException) {}
    }
    @Test fun failedTokenEncryptionPreservesPreviousCiphertext() {
        var stored = byteArrayOf(42)
        val writer = TokenWriter({ throw IllegalStateException("Unavailable key") }, { stored = it })
        try { writer.save("replacement"); fail("Must fail") } catch (_: IllegalStateException) {}
        assertArrayEquals(byteArrayOf(42), stored)
        TokenWriter({ byteArrayOf(7, 8) }, { stored = it }).save("replacement")
        assertArrayEquals(byteArrayOf(7, 8), stored)
    }

    private fun waveform(rate: Int, pitch: Int): ShortArray {
        val samples = ArrayList<Short>()
        val audio = SpeechAudio(rate, pitch, 4095) { bytes, n ->
            assertTrue(n <= 4095 && n % 2 == 0)
            for (i in 0 until n step 2) samples.add(((bytes[i].toInt() and 255) or (bytes[i+1].toInt() shl 8)).toShort())
            true
        }
        val input = FloatArray(48000) { (0.5 * sin(2 * PI * 220 * it / 24000)).toFloat() }
        input.asList().chunked(997).forEach { assertTrue(audio.write(it.toFloatArray())) }
        assertTrue(audio.finish())
        return samples.toShortArray()
    }
    private fun frequency(samples: ShortArray): Double {
        val body = samples.sliceArray(2400 until samples.size - 2400)
        val crossings = (1 until body.size).count { body[it-1] <= 0 && body[it] > 0 }
        return crossings * 24000.0 / body.size
    }
    @Test fun independentTempo() {
        val out = waveform(200, 100)
        assertEquals(24000.0, out.size.toDouble(), 1000.0)
        assertEquals(220.0, frequency(out), 5.0)
    }
    @Test fun pitchRequestsIgnoredAtEveryTempo() {
        for (rate in listOf(50, 100, 200)) {
            val original = waveform(rate, 100)
            assertEquals(48000.0 * 100 / rate, original.size.toDouble(), 1000.0)
            assertEquals(220.0, frequency(original), 5.0)
            for (pitch in listOf(Int.MIN_VALUE, 0, 50, 150, 200, Int.MAX_VALUE)) {
                val out = waveform(rate, pitch)
                assertArrayEquals("pitch=$pitch rate=$rate", original, out)
                val audio = SpeechAudio(rate, pitch, 4096) { _, _ -> true }
                assertEquals(1f, audio.effectivePitch, 0f)
            }
        }
    }
    @Test fun mappingAndPassthrough() {
        assertEquals(0.5f, SpeechAudio.factor(1), 0f)
        assertEquals(2f, SpeechAudio.factor(900), 0f)
        assertEquals(1.25f, SpeechAudio.factor(125), 0f)
        val out = waveform(100,100)
        assertEquals(48000, out.size)
        assertEquals(220.0, frequency(out), 2.0)
        var calls = 0
        val audio = SpeechAudio(100,100,3) { _, n -> assertEquals(2,n); calls++; false }
        assertFalse(audio.write(floatArrayOf(1f, 0f)))
        assertEquals(1,calls)
        assertFalse(SpeechAudio(100,100,1) { _, _ -> true }.accepting)
    }
    @Test fun redirectsAndToken() {
        assertNotNull(DownloadPolicy.authorization("https://huggingface.co/a", "hf_test"))
        assertNull(DownloadPolicy.authorization("https://cas-bridge.xethub.hf.co/a", "hf_test"))
        for (url in listOf("http://huggingface.co/a", "https://evil.example/a", "https://huggingface.co@evil.example/a", "https://user@huggingface.co/a", "https://huggingface.co:8443/a", "https://huggingface.co.evil.example/a")) {
            try { DownloadPolicy.validate(url); fail(url) } catch (_: IllegalArgumentException) {}
        }
        val file = ModelFile("x",3,"unused")
        assertEquals("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", DownloadPolicy.hex(DownloadPolicy.digest(file).digest("abc".toByteArray())))
        assertEquals("f2ba8f84ab5c1bce84a7b441cb1959cfc7093b7f", DownloadPolicy.hex(DownloadPolicy.digest(file.copy(gitBlob=true)).digest("abc".toByteArray())))
    }
    @Test fun pinnedWeightsCdnRequiresTlsAndNeverReceivesToken() {
        val cdn = "https://us.aws.cdn.hf.co/weights?signature=fixture"
        assertEquals("us.aws.cdn.hf.co", DownloadPolicy.validate(cdn).host)
        assertEquals(443, DownloadPolicy.validate("https://us.aws.cdn.hf.co:443/weights").port)
        assertNull(DownloadPolicy.authorization(cdn, "hf_test"))
        assertEquals("Bearer hf_test", DownloadPolicy.authorization("https://huggingface.co/weights", "hf_test"))
        for (url in listOf("http://us.aws.cdn.hf.co/weights", "https://us.aws.cdn.hf.co:8443/weights",
            "https://us.aws.cdn.hf.co.evil.example/weights", "https://other.aws.cdn.hf.co/weights",
            "https://user@us.aws.cdn.hf.co/weights", "$cdn#fragment")) {
            try { DownloadPolicy.validate(url); fail(url) } catch (_: IllegalArgumentException) {}
        }
    }
    @Test fun incompleteAndTamperedFilesRejected() {
        val expected = ModelFile("fixture", 3, "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad")
        DownloadPolicy.verify(expected, 3, DownloadPolicy.digest(expected).digest("abc".toByteArray()))
        for ((count, data) in listOf(2L to "ab", 4L to "abcd", 3L to "xyz")) {
            try { DownloadPolicy.verify(expected, count, DownloadPolicy.digest(expected).digest(data.toByteArray())); fail() }
            catch (_: IllegalArgumentException) {}
        }
    }
    @Test fun rollbackAndRecovery() {
        val root = Files.createTempDirectory("checkpoint").toFile()
        try {
            val target = java.io.File(root,"model").apply { mkdir(); resolve("old").writeText("retained") }
            val staging = java.io.File(root,"model.staging").apply { mkdir(); resolve("new").writeText("candidate") }
            try { CheckpointSwap.replace(staging,target) { error("native load failed") }; fail() } catch (_: IllegalStateException) {}
            assertEquals("retained",target.resolve("old").readText())
            assertFalse(target.resolve("new").exists())
            assertTrue(target.renameTo(java.io.File(root,"model.backup")))
            target.mkdir(); target.resolve("new").writeText("interrupted")
            CheckpointSwap.recover(target)
            assertTrue(target.resolve("old").exists()); assertFalse(target.resolve("new").exists())
        } finally { root.deleteRecursively() }
    }
}
