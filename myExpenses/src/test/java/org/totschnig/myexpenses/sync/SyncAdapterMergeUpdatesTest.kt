package org.totschnig.myexpenses.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.sync.json.TransactionChange

@RunWith(RobolectricTestRunner::class)
class SyncAdapterMergeUpdatesTest : SyncAdapterBaseTest() {
    @Test
    fun shouldReturnSameElement() {
        val change = buildUpdated("random")
        val changes = listOf(change)
        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge).isEqualTo(change)
    }

    @Test
    fun shouldThrowOnEmptyList() {
        mergeUpdatesAndExpectIllegalStateException(emptyList())
    }

    @Test
    fun shouldReturnFirstDeleted() {
        val changes = listOf(
            buildDeleted("random"),
            buildUpdated("random")
        )
        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge.isDelete).isTrue()
    }

    @Test
    fun shouldReturnSecondDeleted() {
        val changes = listOf(
            buildUpdated("random"),
            buildDeleted("random")
        )
        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge.isDelete).isTrue()
    }

    @Test
    fun shouldThrowOnUpdatesWithDistinctUuids() {
        val changes = listOf(
            buildUpdated("one"),
            buildUpdated("two")
        )
        mergeUpdatesAndExpectIllegalStateException(changes)
    }

    private fun mergeUpdatesAndExpectIllegalStateException(changes: List<TransactionChange>) {
        try {
            syncDelegate.mergeUpdates(changes)
            Assert.fail("Expected IllegalStateException to be thrown")
        } catch (_: IllegalStateException) {
            //expected
        }
    }

    @Test
    fun shouldMergeTwoDifferentFields() {
        val uuid = "one"
        val comment = "My comment"
        val amount = 123L

        val changes = listOf(
            buildUpdated(uuid).copy(comment = comment),
            buildUpdated(uuid).copy(amount = amount)
        )
        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge.comment).isEqualTo(comment)
        assertThat(merge.amount).isEqualTo(amount)
    }

    @Test
    fun lastChangeShouldOverride() {
        val uuid = "one"
        val comment1 = "My earlier comment"
        val comment2 = "My later comment"
        val later = System.currentTimeMillis()
        val earlier = later - 10000
        val changes = listOf(
            TransactionChange(
                type = TransactionChange.Type.updated,
                uuid = uuid,
                timeStamp = later,
                comment = comment2
            ),
            TransactionChange(
                type = TransactionChange.Type.updated,
                uuid = uuid,
                timeStamp = earlier,
                comment = comment1
            )
        )


        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge.comment).isEqualTo(comment2)
    }
}