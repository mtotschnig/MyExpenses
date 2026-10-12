package org.totschnig.myexpenses.test.provider

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.provider.TransactionProvider

@RunWith(AndroidJUnit4::class)
class DatabaseEncryptionTest {

    @Test
    fun databaseInfo_whenEncrypted_usesSqlCipherVendor() {
        val targetContext = InstrumentationRegistry.getInstrumentation().targetContext
        val prefHandler = targetContext.injector.prefHandler()

        // Test assumes database encryption is enabled
        Assume.assumeTrue("Test requires encrypted database preference", prefHandler.encryptDatabase)

        val bundle = targetContext.contentResolver.call(
            TransactionProvider.DUAL_URI,
            TransactionProvider.METHOD_GET_DATABASE_INFO,
            null,
            null
        )

        assertThat(bundle).isNotNull()
        val vendor = bundle?.getString(TransactionProvider.KEY_DATABASE_VENDOR)
        val sqliteVersion = bundle?.getString(TransactionProvider.KEY_SQLITE_VERSION)

        assertThat(vendor).isNotNull()
        assertThat(vendor).contains("SQLCipher")
        assertThat(sqliteVersion).isNotNull()
    }
}
