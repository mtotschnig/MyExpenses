package org.totschnig.myexpenses.provider

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.shared_test.CursorSubject.Companion.useAndAssert

@RunWith(RobolectricTestRunner::class)
class SyncHandlerTest : BaseTestWithRepository() {
    private lateinit var provider: BaseTransactionProvider
    private lateinit var context: Context
    private var accountId: Long = 0
    private fun prepareSyncFile(context: Context, resourceName: String) {
        // Read the JSON content from your test resources
        val jsonContent = javaClass.classLoader!!
            .getResourceAsStream(resourceName)
            .bufferedReader()
            .use { it.readText() }

        // Get the file that the provider will read from
        val syncFile = SyncContract.getSyncFile(context)

        // Write the test JSON to that file
        syncFile.writeText(jsonContent)
    }

    @Before
    fun setUp() {
        provider = Robolectric.buildContentProvider(TransactionProvider::class.java).create().get()
        context = ApplicationProvider.getApplicationContext()
        accountId = insertAccount("Test Account")
    }

    @Test
    fun `should correctly create a new transaction`() {
        prepareSyncFile(context, "sync_create_new_transaction.json")

        val extras = Bundle().apply {
            putLong(KEY_ACCOUNTID, accountId)
            putString(KEY_CURRENCY, currencyContext.homeCurrencyString)
            putLong(KEY_TYPE, 1L)
        }

        provider.applyChangesFromSync(extras)

        provider.query(
            TransactionProvider.TRANSACTIONS_URI,
            null,
            "$KEY_UUID = ?",
            arrayOf("c1385e32-3d8c-4ed8-b1f2-0c461934e28e"),
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
        }
        repository
    }

    @Test
    fun `should correctly create a new split transaction`() {
        prepareSyncFile(context, "sync_create_new_split_transaction_legacy.json")

        val extras = Bundle().apply {
            putLong(KEY_ACCOUNTID, accountId)
            putString(KEY_CURRENCY, currencyContext.homeCurrencyString)
            putLong(KEY_TYPE, 1L)
        }

        provider.applyChangesFromSync(extras)

        provider.query(
            TransactionProvider.TRANSACTIONS_URI,
            null,
            "$KEY_UUID = ?",
            arrayOf("7bf6c4ed-09d2-4237-ae6c-0ec0b8fe41c3"), // Use the UUID from the test JSON
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
        }
    }

    @After
    fun tearDown() {
        SyncContract.getSyncFile(context).delete()
    }
}