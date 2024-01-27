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
        val change = buildUpdated().setUuid("random").build()
        val changes = buildList {
            add(change)
        }
        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge).isEqualTo(change)
    }

    @Test
    fun shouldThrowOnEmptyList() {
        mergeUpdatesAndExpectIllegalStateException(emptyList())
    }

    @Test
    fun shouldReturnFirstDeleted() {
        val changes = buildList {
            add(buildDeleted().setUuid("random").build())
            add(buildUpdated().setUuid("random").build())
        }
        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge.isDelete).isTrue()
    }

    @Test
    fun shouldReturnSecondDeleted() {
        val changes = buildList {
            add(buildUpdated().setUuid("random").build())
            add(buildDeleted().setUuid("random").build())
        }
        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge.isDelete).isTrue()
    }

    @Test
    fun shouldThrowOnUpdatesWithDistinctUuids() {
        val changes = buildList {
            add(buildUpdated().setUuid("one").build())
            add(buildUpdated().setUuid("two").build())
        }
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
        val uuid = "one"
        val comment = "My comment"
        val amount = 123L
        val changes = buildList {
            add(buildUpdated().setUuid(uuid).setComment(comment).build())
            add(buildUpdated().setUuid(uuid).setAmount(amount).build())
        }
        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge.comment()).isEqualTo(comment)
        assertThat(merge.amount()).isEqualTo(amount)
    }

    @Test
    fun lastChangeShouldOverride() {
        val uuid = "one"
        val comment1 = "My earlier comment"
        val comment2 = "My later comment"
        val later = System.currentTimeMillis()
        val earlier = later - 10000
        val changes = buildList {
            add(
                TransactionChange.builder()
                    .setType(TransactionChange.Type.updated)
                    .setUuid(uuid)
                    .setTimeStamp(later)
                    .setComment(comment2)
                    .build()
            )
            add(
                TransactionChange.builder()
                    .setType(TransactionChange.Type.updated)
                    .setUuid(uuid)
                    .setTimeStamp(earlier)
                    .setComment(comment1)
                    .build()
            )
        }

        val merge = syncDelegate.mergeUpdates(changes)
        assertThat(merge.comment()).isEqualTo(comment2)
    }
}