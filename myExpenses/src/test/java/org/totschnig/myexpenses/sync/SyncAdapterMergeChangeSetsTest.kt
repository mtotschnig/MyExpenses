package org.totschnig.myexpenses.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.sync.json.TransactionChange

@RunWith(RobolectricTestRunner::class)
class SyncAdapterMergeChangeSetsTest : SyncAdapterBaseTest() {
    @Test
    fun listsWithoutMatchingShouldBeReturnedIdentical() {
        val first = listOf(
            buildCreated("random1")
                .copy(amount = 123)
        )
        val second = listOf(
            buildCreated("random2")
                .copy(comment = "Commentary")
        )
        val result = syncDelegate.mergeChangeSets(first, second)
        assertThat(result.first).isEqualTo(first)
        assertThat(result.second).isEqualTo(second)
    }

    @Test
    fun listsWithMatchingWithoutConflictShouldBeReturnedIdentical() {
        val first = listOf(
            buildCreated("random1")
                .copy(amount = 123)
        )
        val second = listOf(
            buildCreated("random1")
                .copy(comment = "Commentary")
        )
        val result = syncDelegate.mergeChangeSets(first, second)
        assertThat(result.first).isEqualTo(first)
        assertThat(result.second).isEqualTo(second)
    }

    @Test
    fun listsWithConflictShouldHonourLast() {
        val first = listOf(
            buildUpdated("random1").copy(
                timeStamp = 1,
                comment = "Kommentar"
            )
        )
        val second = listOf(
            buildUpdated("random1").copy(
                timeStamp = 2, comment = "Commentary"
            )
        )
        val result: Pair<List<TransactionChange>, List<TransactionChange>> =
            syncDelegate.mergeChangeSets(first, second)
        assertThat(result.first.first().comment).isNull()
        assertThat(result.second.first().comment).isEqualTo("Commentary")
    }

    @Test
    fun deleteInSameSetShouldTriggerRemovalOfRelatedChanges() {
        val uuid = "random"
        val deleteChange = buildDeleted(uuid)
        val first = listOf(
            buildCreated(uuid),
            deleteChange
        )
        val result = syncDelegate.mergeChangeSets(first, emptyList())
        assertThat(result.first).hasSize(1)
        assertThat(result.first.first()).isEqualTo(deleteChange)
        assertThat(result.second).isEmpty()
    }

    @Test
    fun cudWithSpecials() {
        val created = buildCreated("random1")
        val updated = buildUpdated("random1")
        val special = buildSpecial("random1")
        val first = listOf(
            created,
            updated,
            special
        )
        val result = syncDelegate.mergeChangeSets(first, emptyList())
        assertThat(result.first).isEqualTo(listOf(created, special))
    }

    @Test
    fun onlySpecials() {
        val special = buildSpecial("random1")
        val first = listOf(special)
        val result = syncDelegate.mergeChangeSets(first, emptyList())
        assertThat(result.first).isEqualTo(first)
    }

    @Test
    fun deleteInDifferentSetShouldTriggerRemovalOfRelatedChanges() {
        val uuid = "random"
        val first = listOf(buildUpdated(uuid))
        val second = listOf(buildDeleted(uuid))
        val result = syncDelegate.mergeChangeSets(first, second)
        assertThat(result.first).isEmpty()
        assertThat(result.second).isEqualTo(second)
    }

    @Test
    fun updatesShouldBeMerged() {
        val uuid = "random"
        val first = listOf(
            buildUpdated(uuid),
            buildUpdated(uuid)
        )
        val result = syncDelegate.mergeChangeSets(first, emptyList())
        assertThat(result.first).hasSize(1)
    }

    @Test
    fun insertCanBeMergedWithUpdate() {
        val uuid = "random"
        val first = listOf(
            buildCreated(uuid),
            buildUpdated(uuid)
        )
        val result = syncDelegate.mergeChangeSets(first, emptyList())
        assertThat(result.first).hasSize(1)
    }
}