package org.totschnig.myexpenses.sync

import android.content.ContentProviderOperation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.provider.SyncHandler
import org.totschnig.myexpenses.sync.json.TagInfo
import org.totschnig.myexpenses.sync.json.TransactionChange

@RunWith(RobolectricTestRunner::class)
class SyncAdapterWriteToDbTest: BaseTestWithRepository() {
    private lateinit var syncDelegate: SyncHandler
    private lateinit var ops: ArrayList<ContentProviderOperation>

    @Before
    fun setup() {
        ops = ArrayList()
    }

    private fun setupSync() {
        syncDelegate = SyncHandler(0, currencyContext["EUR"], 1, repository, currencyContext)
    }

    private fun setupSyncWithFakeResolver() {
        syncDelegate = SyncHandler(0, currencyContext["EUR"], 1, repository, currencyContext) { _, _ -> 1 }
    }

    @Test
    fun createdChangeShouldBeCollectedAsInsertOperation() {
        setupSync()
        val change = TransactionChange(
            type = TransactionChange.Type.created,
            uuid = "any",
            timeStamp = TransactionChange.currentTimStamp,
            amount = 123L
        )
        val ops = syncDelegate.collectOperations(change)
        assertThat(ops).hasSize(1)
        assertThat(ops[0].isInsert).isTrue()
    }

    @Test
    fun createChangeWithTag() {
        setupSync()
        val change = TransactionChange(
            type = TransactionChange.Type.created,
            uuid = "any",
            timeStamp = TransactionChange.currentTimStamp,
            amount = 123L,
            tagsV2 = setOf(TagInfo("tag"))
        )
        val ops = syncDelegate.collectOperations(change)
        assertThat(ops).hasSize(2)
        assertThat(ops[0].isInsert).isTrue()
        assertThat(ops[1].uri.path).isEqualTo("/transactions/tags")
        assertThat(ops[1].isInsert).isTrue()
    }

    @Test
    fun deletedChangeShouldBeCollectedAsDeleteOperation() {
        setupSyncWithFakeResolver()
        val change = TransactionChange(
            type = TransactionChange.Type.deleted,
            uuid = "any",
            timeStamp = TransactionChange.currentTimStamp
        )
        val ops = syncDelegate.collectOperations(change)
        assertThat(ops).hasSize(1)
        assertThat(ops[0].isDelete).isTrue()
    }
}