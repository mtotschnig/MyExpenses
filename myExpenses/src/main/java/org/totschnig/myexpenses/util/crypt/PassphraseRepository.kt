package org.totschnig.myexpenses.util.crypt

import android.content.Context
import android.os.Build
import org.apache.commons.text.RandomStringGenerator
import timber.log.Timber
import java.io.File
import java.nio.charset.Charset
import java.security.SecureRandom

class PassphraseRepository(private val context: Context) {
    fun getPassphrase(): ByteArray {
        val file = File(context.filesDir, "fints_passphrase.bin")
        val encryptedFile = androidx.security.crypto.EncryptedFile.Builder(
            context,
            file,
            androidx.security.crypto.MasterKey.Builder(context)
                .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM).build(),
            androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return if (file.exists()) {
            encryptedFile.openFileInput().use { it.readBytes() }.also {
                Timber.d("passphrase loaded: ${it.toString(Charsets.UTF_8)}")
            }
        } else {
            generatePassphrase().also { passphrase ->
                Timber.d("passphrase generated: ${passphrase.toString(Charsets.UTF_8)}")
                encryptedFile.openFileOutput().use { it.write(passphrase) }
            }
        }
    }

    private fun generatePassphrase(): ByteArray {
        val random = SecureRandom()
        val generator = RandomStringGenerator.builder().usingRandom {
            random.nextInt(it)
        }
            .withinRange('a'.code, 'z'.code).get()
        return generator.generate(20).toByteArray()
    }
}