package ai.gradium.phonon.tts

import java.io.File
import java.security.MessageDigest
import java.util.concurrent.CancellationException

/** A language transaction never touches a checkpoint or another language. */
internal class VoicePackStore(private val root: File) {
    fun target(language: String): File { require(language in listOf("en", "de", "fr", "es", "pt")); return File(root, language) }
    fun ready(language: String, entries: List<BundledVoice>): Boolean {
        val dir = target(language)
        return File(dir, "complete").isFile && entries.isNotEmpty() && entries.all { verified(File(dir, it.asset), it) }
    }
    fun install(language: String, entries: List<BundledVoice>, cancelled: () -> Boolean, fill: (File) -> Unit) {
        check(entries.isNotEmpty() && entries.all { it.language == language })
        check(root.isDirectory || root.mkdirs())
        val target = target(language)
        CheckpointSwap.recover(target)
        val staging = File(root, "$language.staging")
        check(!staging.exists() || staging.deleteRecursively()); check(staging.mkdirs())
        try {
            fill(staging)
            check(entries.all { verified(File(staging, it.asset), it) }) { "Voice pack integrity check failed" }
            File(staging, "complete").writeText(entries.joinToString("\n") { "${it.asset} ${it.bytes} ${it.sha256}" })
            if (cancelled()) throw CancellationException()
            CheckpointSwap.replace(staging, target) { if (cancelled()) throw CancellationException() }
        } finally { staging.deleteRecursively() }
    }
    companion object {
        private val verificationCache = mutableMapOf<Pair<String, String>, Pair<Long, Long>>()
        fun cachedVerified(file: File, entry: BundledVoice): Boolean = synchronized(verificationCache) {
            val key = file.absolutePath to entry.sha256
            val stamp = file.length() to file.lastModified()
            if (file.isFile && stamp.first == entry.bytes && verificationCache[key] == stamp) true
            else verified(file, entry).also {
                if (it) verificationCache[key] = stamp else verificationCache.remove(key)
            }
        }
        /** Check both locations before skipping; never replace a canonical collision. */
        fun existingCanonical(checkpoint: File, entry: BundledVoice): Boolean {
            val existing = listOf("voices", "embeddings").map { File(checkpoint, "$it/${entry.stem}.safetensors") }.filter { it.exists() }
            for (file in existing) check(cachedVerified(file, entry)) { "Voice integrity check failed: ${file.name}" }
            return existing.isNotEmpty()
        }

        fun verified(file: File, entry: BundledVoice): Boolean = file.isFile && file.length() == entry.bytes &&
            DownloadPolicy.hex(MessageDigest.getInstance("SHA-256").digest(file.readBytes())) == entry.sha256
    }
}
