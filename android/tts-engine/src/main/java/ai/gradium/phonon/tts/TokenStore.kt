package ai.gradium.phonon.tts

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Authenticated, versioned envelope; independent of Android for focused JVM tests. */
internal object TokenEnvelope {
    fun seal(value: String, key: SecretKey): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return byteArrayOf(1, cipher.iv.size.toByte()) + cipher.iv + cipher.doFinal(value.toByteArray(Charsets.UTF_8))
    }
    fun open(bytes: ByteArray, key: SecretKey): String {
        require(bytes.size >= 30 && bytes[0] == 1.toByte() && bytes[1] == 12.toByte()) { "Invalid token envelope" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key, GCMParameterSpec(128, bytes.copyOfRange(2, 14)))
        return String(cipher.doFinal(bytes.copyOfRange(14, bytes.size)), Charsets.UTF_8)
    }
}

/** Storage seam: encryption must succeed before the atomic replacement is invoked. */
internal class TokenWriter(private val encrypt: (String) -> ByteArray, private val replace: (ByteArray) -> Unit) {
    fun save(value: String) { require(value.isNotBlank()); val encrypted = encrypt(value); replace(encrypted) }
}

class TokenStore(context: Context) {
    private val file = AtomicFile(File(context.noBackupFilesDir, "hf-token.enc"))
    private val alias = "phonon.hf-token.v1"
    private fun key(create: Boolean): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (store.getKey(alias, null) as? SecretKey)?.let { return it }
        check(create) { "Token key unavailable" }
        return KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore").apply {
            init(KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM).setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).build())
        }.generateKey()
    }
    @Synchronized fun exists() = file.baseFile.exists() || File(file.baseFile.path + ".bak").exists()
    @Synchronized fun read(): String? = if (exists()) TokenEnvelope.open(file.readFully(), key(false)) else null
    @Synchronized fun save(value: String) {
        TokenWriter({ TokenEnvelope.seal(it, key(true)) }) { encrypted ->
            val output = file.startWrite()
            try { output.write(encrypted); file.finishWrite(output) }
            catch (t: Throwable) { file.failWrite(output); throw t }
        }.save(value)
    }
    @Synchronized fun delete() {
        file.delete()
        // Explicit deletion also permits recovery after permanent key invalidation.
        KeyStore.getInstance("AndroidKeyStore").apply { load(null); deleteEntry(alias) }
    }
}
