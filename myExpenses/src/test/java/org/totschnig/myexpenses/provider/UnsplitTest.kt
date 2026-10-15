package org.totschnig.myexpenses.provider

import android.content.ContentValues
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.db2.RepositoryTransaction
import org.totschnig.myexpenses.db2.createSplitTransaction
import org.totschnig.myexpenses.db2.entities.Transaction
import org.totschnig.myexpenses.db2.loadTransactions
import org.totschnig.myexpenses.model.generateUuid
import org.totschnig.myexpenses.provider.TransactionProvider.UNSPLIT_URI

@RunWith(RobolectricTestRunner::class)
class UnsplitTest : TransactionProviderBaseTest() {
    val uuidPart1 = generateUuid()
    val uuidPart2 = generateUuid()
    lateinit var transaction: RepositoryTransaction

    @Before
    fun setupSplitTransaction() {
        transaction = repository.createSplitTransaction(
            parentTransaction = Transaction(
                categoryId = SPLIT_CATID,
                amount = 100L,
                accountId = accountId,
                uuid = generateUuid()
            ),
            splitTransactions = listOf(
                Transaction(
                    amount = 50L,
                    accountId = accountId,
                    uuid = uuidPart1
                ),
                Transaction(
                    amount = 50L,
                    accountId = accountId,
                    uuid = uuidPart2
                )
            )
        )
    }

    @Test
    fun unsplitByRowId() = runTest {
        testUnsplit {
            it.put(KEY_ROWID, transaction.id)
        }
    }

    //used from SyncAdapter
    @Test
    fun unsplitByUuId() = runTest {
        testUnsplit {
            it.put(KEY_UUID, transaction.data.uuid)
        }
    }

    private suspend fun testUnsplit(configureValues: (ContentValues) -> Unit) {
        assertThat(
            provider.update(
                UNSPLIT_URI,
                ContentValues(1).apply { configureValues(this) },
                null
            )
        ).isEqualTo(3)
        val transactions = repository.loadTransactions(accountId)
        assertThat(transactions.all { it.parentId == null } ).isTrue()
        assertThat(transactions.map { it.uuid }).containsExactly(uuidPart1, uuidPart2)
    }
}