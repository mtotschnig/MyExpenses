package org.totschnig.myexpenses.test.util

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.util.crypt.PrefKeyStoreEncryption

@RunWith(AndroidJUnit4::class)
class PrefKeyStoreEncryptionTest {

    @Test
    fun encryptAndDecryptRoundTrip() {
        val original = "secret_password_123"
        val encrypted = PrefKeyStoreEncryption.encrypt(original)
        val decrypted = PrefKeyStoreEncryption.decrypt(encrypted!!)
        assertEquals(original, decrypted)
    }

    @Test
    fun decryptLegacyUnencryptedStringReturnsOriginal() {
        val legacyPlaintext = "plain_unencrypted_password"
        val decrypted = PrefKeyStoreEncryption.decrypt(legacyPlaintext)
        assertEquals(legacyPlaintext, decrypted)
    }

    @Test
    fun decryptCorruptedEncryptedStringReturnsNull() {
        val invalidEncryptedString = "ME_ENC_01:invalid_base64_payload_from_old_install"
        val decrypted = PrefKeyStoreEncryption.decrypt(invalidEncryptedString)
        assertNull(decrypted)
    }
}
