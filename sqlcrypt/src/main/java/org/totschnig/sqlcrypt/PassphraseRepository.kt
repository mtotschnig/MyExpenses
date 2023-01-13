/*
  Copyright (c) 2021 CommonsWare, LLC

  Licensed under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License. You may obtain	a copy
  of the License at http://www.apache.org/licenses/LICENSE-2.0. Unless required
  by applicable law or agreed to in writing, software distributed under the
  License is distributed on an "AS IS" BASIS,	WITHOUT	WARRANTIES OR CONDITIONS
  OF ANY KIND, either express or implied. See the License for the specific
  language governing permissions and limitations under the License.

  Covered in detail in the book _Elements of Android Room_

  https://commonsware.com/Room
*/

package org.totschnig.sqlcrypt

import android.content.Context
import android.os.Build
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.security.SecureRandom

private const val PASSPHRASE_LENGTH = 32

class PassphraseRepository(private val context: Context) {
    fun getPassphrase(): ByteArray {
        val file = File(context.filesDir, "passphrase.bin")
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()

        return if (file.exists()) {
            encryptedFile.openFileInput().use { it.readBytes() }
        } else {
            generatePassphrase().also { passphrase ->
                encryptedFile.openFileOutput().use { it.write(passphrase) }
            }
        }
    }

    private fun generatePassphrase(): ByteArray {
        val random = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            SecureRandom.getInstanceStrong()
        } else {
            SecureRandom()
        }
        val result = ByteArray(PASSPHRASE_LENGTH)

        random.nextBytes(result)

        // filter out zero byte values, as SQLCipher does not like them
        while (result.contains(0)) {
            random.nextBytes(result)
        }

        return result
    }
}