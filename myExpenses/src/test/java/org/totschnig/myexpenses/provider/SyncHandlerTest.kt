package org.totschnig.myexpenses.provider

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
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
    fun `applyChangesFromSync should correctly create a new transaction`() {
        // Arrange: Stage the JSON file for this specific test case
        prepareSyncFile(context, "sync_create_new_transaction.json")

        val extras = Bundle().apply {
            putLong(KEY_ACCOUNTID, accountId)
            putString(KEY_CURRENCY, currencyContext.homeCurrencyString)
            putLong(KEY_TYPE, 1L)
        }

        // Act: Run the method you want to test
        provider.applyChangesFromSync(extras)

        // Assert: Query the database to verify the new transaction was created correctly
        provider.query(
            TransactionProvider.TRANSACTIONS_URI,
            null,
            "$KEY_UUID = ?",
            arrayOf("c1385e32-3d8c-4ed8-b1f2-0c461934e28e"), // Use the UUID from the test JSON
            null
        ).useAndAssert {
            hasCount(1)
            movesToFirst()
        }
    }

    @After
    fun tearDown() {
        // Clean up the temporary sync file
        SyncContract.getSyncFile(context).delete()
    }
}