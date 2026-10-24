package ai.gradium.phonon.tts

import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.CancellationException
import javax.net.ssl.HttpsURLConnection

/** No caller-supplied URLs; manual, finite redirects and a fixed TLS host allowlist. */
object DownloadPolicy {
    private val hosts = setOf("huggingface.co", "cdn-lfs.huggingface.co", "cdn-lfs.hf.co", "cdn-lfs-us-1.hf.co",
        "cdn-lfs-eu-1.hf.co", "us.aws.cdn.hf.co", "cas-bridge.xethub.hf.co", "cas-server.xethub.hf.co", "transfer.xethub.hf.co")
    fun validate(url: String): URI = URI(url).also {
        require(it.scheme == "https" && it.host in hosts && it.rawUserInfo == null &&
            (it.port == -1 || it.port == 443) && it.fragment == null) { "Unsafe download redirect" }
    }
    fun authorization(url: String, token: String?): String? =
        token?.takeIf { validate(url).host == "huggingface.co" && it.isNotBlank() }?.let { "Bearer $it" }
    fun digest(file: ModelFile): MessageDigest = MessageDigest.getInstance(if (file.gitBlob) "SHA-1" else "SHA-256").apply {
        if (file.gitBlob) update("blob ${file.size}\u0000".toByteArray(Charsets.US_ASCII))
    }
    fun verify(file: ModelFile, count: Long, digest: ByteArray) {
        require(count == file.size) { "Incomplete download" }
        require(hex(digest) == file.hash) { "File integrity check failed: ${file.name}" }
    }
    fun hex(bytes: ByteArray) = bytes.joinToString("") { "%02x".format(it.toInt() and 255) }
}

class SecureDownload(private val cancelled: () -> Boolean, private val progress: (Long, Long) -> Unit) {
    @Volatile private var active: HttpsURLConnection? = null
    fun cancel() { active?.disconnect() }
    private fun checkCancelled() { if (cancelled()) throw CancellationException() }

    fun download(model: DownloadModel, token: String?, into: File) {
        downloadFiles(model.repo, model.revision, model.files, token, into)
    }

    fun downloadFiles(repo: String, revision: String, files: List<ModelFile>, token: String?, into: File) {
        require(repo.matches(Regex("gradium/[A-Za-z0-9-]+")) && revision.matches(Regex("[a-f0-9]{40}")))
        val total = files.sumOf { it.size }
        require(files.all { it.name.matches(Regex("(?:voices/)?[A-Za-z0-9_.-]+")) && ".." !in it.name && it.size > 0 })
        require(token == null || token.matches(Regex("hf_[A-Za-z0-9]{1,508}"))) { "Invalid HF read token format" }
        require(total <= 80_000_000) { "Model exceeds size limit" }
        require(into.usableSpace > total + 32L * 1024 * 1024) { "Not enough storage" }
        var completed = 0L
        for (file in files) {
            checkCancelled()
            var url = "https://huggingface.co/${repo}/resolve/${revision}/${file.name}"
            var connection: HttpsURLConnection? = null
            try {
                for (hop in 0..5) {
                    checkCancelled()
                    DownloadPolicy.validate(url)
                    val conn = URL(url).openConnection() as HttpsURLConnection
                    connection = conn; active = conn
                    conn.instanceFollowRedirects = false
                    conn.connectTimeout = 20_000; conn.readTimeout = 20_000
                    conn.setRequestProperty("Accept-Encoding", "identity")
                    DownloadPolicy.authorization(url, token)?.let { conn.setRequestProperty("Authorization", it) }
                    val status = conn.responseCode
                    if (status in listOf(301, 302, 303, 307, 308)) {
                        val location = conn.getHeaderField("Location") ?: error("Missing redirect")
                        require(hop < 5) { "Too many redirects" }
                        url = DownloadPolicy.validate(url).resolve(location).toString()
                        DownloadPolicy.validate(url)
                        conn.disconnect(); connection = null; active = null
                        continue
                    }
                    if (status == 401 || status == 403) error("401/403: Accept the model terms on Hugging Face and enter your own read token.")
                    require(status == 200) { "Download HTTP $status. Check network and retry." }
                    require(conn.contentType?.lowercase()?.contains("text/html") != true) { "Server returned HTML" }
                    require(conn.contentLengthLong == -1L || conn.contentLengthLong == file.size) { "Unexpected file length" }
                    val part = File(into, file.name + ".part")
                    check(part.parentFile!!.isDirectory || part.parentFile!!.mkdirs())
                    try {
                        val digest = DownloadPolicy.digest(file)
                        var count = 0L
                        conn.inputStream.use { input ->
                            FileOutputStream(part).use { output ->
                                val buffer = ByteArray(64 * 1024)
                                while (true) {
                                    checkCancelled()
                                    val n = input.read(buffer)
                                    if (n == -1) break
                                    count += n
                                    require(count <= file.size) { "Download exceeds size limit" }
                                    digest.update(buffer, 0, n); output.write(buffer, 0, n)
                                    progress(completed + count, total)
                                }
                                output.fd.sync()
                            }
                        }
                        DownloadPolicy.verify(file, count, digest.digest())
                        checkCancelled()
                        check(part.renameTo(File(into, file.name))) { "Cannot save model file" }
                    } finally { part.delete() }
                    completed += file.size
                    break
                }
            } finally { connection?.disconnect(); active = null }
        }
    }
}
