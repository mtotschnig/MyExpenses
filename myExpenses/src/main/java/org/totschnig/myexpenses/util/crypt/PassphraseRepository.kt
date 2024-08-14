/*
  Based on https://commonsware.com/Room/pages/chap-passphrase-001.html
  licensed under the Creative Commons Attribution-ShareAlike 4.0 International license.
*/

package org.totschnig.myexpenses.util.crypt

import android.content.Context
import java.io.File

class PassphraseRepository(context: Context, val file: File, val generator: () -> ByteArray) {

    private val encryptedFile = androidx.security.crypto.EncryptedFile.Builder(
        context,
        file,
        androidx.security.crypto.MasterKey.Builder(context)
            .setKeyScheme(androidx.security.crypto.MasterKey.KeyScheme.AES256_GCM).build(),
        androidx.security.crypto.EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
    ).build()

    fun getPassphrase(): ByteArray = if (file.exists())
        encryptedFile.openFileInput().use { it.readBytes() }
    else
        generator().also { passphrase -> storePassphrase(passphrase) }

    fun storePassphrase(passphrase: ByteArray) {
        encryptedFile.openFileOutput().use { it.write(passphrase) }
    }
}