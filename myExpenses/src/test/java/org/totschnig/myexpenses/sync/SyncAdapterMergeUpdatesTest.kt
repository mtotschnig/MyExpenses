package org.totschnig.myexpenses.sync

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.sync.json.TransactionChange

@RunWith(RobolectricTestRunner::class)
class SyncAdapterMergeUpdatesTest : SyncAdapterBaseTest() {
    @Test
    fun shouldReturnSameElement() {
        val changes: MutableList<TransactionChange> = ArrayList()
        val change = buildUpdated().setUuid("random").build()
        changes.add(change)
        val merge = syncDelegate.mergeUpdates(changes)
        Assert.assertEquals(change, merge)
    }

    @Test
    fun shouldThrowOnEmptyList() {
        val changes: List<TransactionChange> = ArrayList()
        mergeUpdatesAndExpectIllegalStateException(changes)
    }

    @Test
    fun shouldThrowOnDelete() {
        val changes: MutableList<TransactionChange> = ArrayList()
        changes.add(buildDeleted().setUuid("random").build())
        changes.add(buildUpdated().setUuid("random").build())
        mergeUpdatesAndExpectIllegalStateException(changes)
    }

    @Test
    fun shouldThrowOnUpdatesWithDistinctUuids() {
        val changes: MutableList<TransactionChange> = ArrayList()
        changes.add(buildUpdated().setUuid("one").build())
        changes.add(buildUpdated().setUuid("two").build())
        mergeUpdatesAndExpectIllegalStateException(changes)
    }

    private fun mergeUpdatesAndExpectIllegalStateException(changes: List<TransactionChange>) {
        try {
            syncDelegate.mergeUpdates(changes)
            Assert.fail("Expected IllegalStateException to be thrown")
        } catch (expected: IllegalStateException) {
            //expected
        }
    }

    @Test
    fun shouldMergeTwoDifferentFields() {
        val changes: MutableList<TransactionChange> = ArrayList()
        val uuid = "one"
        val comment = "My comment"
        val amount = 123L
        changes.add(buildUpdated().setUuid(uuid).setComment(comment).build())
        changes.add(buildUpdated().setUuid(uuid).setAmount(amount).build())
        val merge = syncDelegate.mergeUpdates(changes)
        Assert.assertEquals(comment, merge.comment())
        Assert.assertEquals(amount, merge.amount())
    }

    @Test
    fun lastChangeShouldOverride() {
        val changes: MutableList<TransactionChange> = ArrayList()
        val uuid = "one"
        val comment1 = "My earlier comment"
        val comment2 = "My later comment"
        val later = System.currentTimeMillis()
        val earlier = later - 10000
        changes.add(
            TransactionChange.builder().setType(TransactionChange.Type.updated).setUuid(uuid)
                .setTimeStamp(later).setComment(comment2).build()
        )
        changes.add(
            TransactionChange.builder().setType(TransactionChange.Type.updated).setUuid(uuid)
                .setTimeStamp(earlier).setComment(comment1).build()
        )
        val merge = syncDelegate.mergeUpdates(changes)
        Assert.assertEquals(comment2, merge.comment())
    }
}