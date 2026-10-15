package org.totschnig.myexpenses.provider

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.db2.extractTagId
import org.totschnig.myexpenses.db2.findCategory
import org.totschnig.myexpenses.db2.findParty
import org.totschnig.shared_test.TransactionData
import org.totschnig.shared_test.assertTransaction
import org.totschnig.shared_test.findCategoryPath

@RunWith(RobolectricTestRunner::class)
class SyncHandlerTest : TransactionProviderBaseTest() {
    private lateinit var context: Context

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

    private fun findTransactionByUuid(uuid: String) = provider.query(
        TransactionProvider.TRANSACTIONS_URI,
        arrayOf(KEY_ROWID),
        "$KEY_UUID = ?",
        arrayOf(uuid),
        null
    )!!.useAndMapToOne { it.getLong(0) }!!

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
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

        val id = findTransactionByUuid("c1385e32-3d8c-4ed8-b1f2-0c461934e28e")
        repository.assertTransaction(id, TransactionData(
            comment = "Def",
            accountId = accountId,
            amount = -12300,
            category = repository.findCategoryPath("Financial expenses", "Bank charges"),
            party = repository.findParty("Joe"),
            tags = listOf(repository.extractTagId("Abc"))
        ))
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

        val id = findTransactionByUuid("7bf6c4ed-09d2-4237-ae6c-0ec0b8fe41c3")
        repository.assertTransaction(id, TransactionData(
            comment = "Split",
            accountId = accountId,
            amount = -3300,
            category = SPLIT_CATID,
            splitParts = listOf(
                TransactionData(
                    accountId = accountId,
                    amount = -1100,
                    category = repository.findCategory("Geschenke")
                ),
                TransactionData(
                    accountId = accountId,
                    amount = -2200,
                    category = repository.findCategoryPath("Bekleidung", "Kleidung")
                ),
            )
        ))

    }

    @After
    fun tearDown() {
        SyncContract.getSyncFile(context).delete()
    }
}