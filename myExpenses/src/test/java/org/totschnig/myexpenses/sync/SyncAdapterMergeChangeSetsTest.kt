package org.totschnig.myexpenses.sync

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SyncAdapterMergeChangeSetsTest : SyncAdapterBaseTest() {
    @Test
    fun listsWithoutMatchingShouldBeReturnedIdentical() {
        val first = buildList {
            add(buildCreated().setUuid("random1").setAmount(123).build())
        }
        val second = buildList {
            add(buildCreated().setUuid("random2").setComment("Commentary").build())
        }
        val result = syncDelegate.mergeChangeSets(first, second)
        assertThat(result.first).isEqualTo(first)
        assertThat(result.second).isEqualTo(second)
    }

    @Test
    fun listsWithMatchingWithoutConflictShouldBeReturnedIdentical() {
        val first = buildList {
            add(buildCreated().setUuid("random1").setAmount(123).build())
        }
        val second = buildList {
            add(buildCreated().setUuid("random1").setComment("Commentary").build())
        }
        val result = syncDelegate.mergeChangeSets(first, second)
        assertThat(result.first).isEqualTo(first)
        assertThat(result.second).isEqualTo(second)
    }

    @Test
    fun deleteInSameSetShouldTriggerRemovalOfRelatedChanges() {
        val uuid = "random"
        val deleteChange = buildDeleted().setUuid(uuid).build()
        val first = buildList {
            add(buildCreated().setUuid(uuid).build())
            add(deleteChange)
        }
        val result = syncDelegate.mergeChangeSets(first, emptyList())
        assertThat(result.first).hasSize(1)
        assertThat(result.first.first()).isEqualTo(deleteChange)
        assertThat(result.second).isEmpty()
    }

    @Test
    fun deleteInDifferentSetShouldTriggerRemovalOfRelatedChanges() {
        val uuid = "random"
        val first = buildList {
            add(buildUpdated().setUuid(uuid).build())
        }
        val second = buildList {
            add(buildDeleted().setUuid(uuid).build())
        }
        val result = syncDelegate.mergeChangeSets(first, second)
        assertThat(result.first).isEmpty()
        assertThat(result.second).isEqualTo(second)
    }

    @Test
    fun updatesShouldBeMerged() {
        val uuid = "random"
        val first = buildList {
            add(buildUpdated().setUuid(uuid).build())
            add(buildUpdated().setUuid(uuid).build())
        }
        val result = syncDelegate.mergeChangeSets(first, emptyList())
        assertThat(result.first).hasSize(1)
    }

    @Test
    fun insertCanBeMergedWithUpdate() {
        val uuid = "random"
        val first = buildList {
            add(buildCreated().setUuid(uuid).build())
            add(buildUpdated().setUuid(uuid).build())
        }
        val result = syncDelegate.mergeChangeSets(first, emptyList())
        assertThat(result.first).hasSize(1)
    }
}