package ai.gradium.phonon.tts

import kotlin.math.roundToInt

/** Per-request, bounded streaming PCM processor. Android retains ownership of playback. */
class SpeechAudio(rate: Int, pitch: Int, maxBytes: Int, private val send: (ByteArray, Int) -> Boolean) {
    val effectiveRate = factor(rate)
    val effectivePitch = factor(pitch)
    private val capacity = minOf(maxBytes and -2, 8192)
    private val sonic = if (effectiveRate == 1f && effectivePitch == 1f) null else Sonic(24000, 1).apply {
        setSpeed(effectiveRate)
        setPitch(effectivePitch)
        setQuality(1)
    }
    private val output = ShortArray(maxOf(1, capacity / 2))
    private val bytes = ByteArray(maxOf(2, capacity))
    var accepting = capacity >= 2
        private set

    fun write(pcm: FloatArray): Boolean {
        var offset = 0
        while (offset < pcm.size && accepting) {
            val n = minOf(output.size, pcm.size - offset)
            for (i in 0 until n) {
                val v = pcm[offset + i]
                output[i] = (if (v.isFinite()) v.coerceIn(-1f, 1f) * 32767f else 0f).roundToInt().toShort()
            }
            if (sonic == null) emit(n) else {
                sonic.writeShortToStream(output, n)
                drain()
            }
            offset += n
        }
        return accepting
    }

    fun finish(): Boolean {
        if (accepting && sonic != null) { sonic.flushStream(); drain() }
        return accepting
    }

    private fun drain() {
        while (accepting && sonic!!.samplesAvailable() > 0) emit(sonic.readShortFromStream(output, output.size))
    }

    private fun emit(n: Int) {
        for (i in 0 until n) {
            bytes[i * 2] = output[i].toByte()
            bytes[i * 2 + 1] = (output[i].toInt() shr 8).toByte()
        }
        accepting = send(bytes, n * 2)
    }

    companion object {
        fun factor(percent: Int): Float = (percent / 100f).coerceIn(0.5f, 2f)
    }
}
