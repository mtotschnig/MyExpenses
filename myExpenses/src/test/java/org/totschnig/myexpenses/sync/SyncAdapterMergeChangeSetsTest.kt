package org.totschnig.myexpenses.sync

import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.sync.json.TransactionChange

@RunWith(RobolectricTestRunner::class)
class SyncAdapterMergeChangeSetsTest : SyncAdapterBaseTest() {
    @Test
    fun noConflictsShouldBeReturnedIdentical() {
        val first: MutableList<TransactionChange> = ArrayList()
        first.add(buildCreated().setUuid("random1").build())
        val second: MutableList<TransactionChange> = ArrayList()
        second.add(buildCreated().setUuid("random2").build())
        val result = syncDelegate.mergeChangeSets(first, second)
        Assert.assertEquals(first, result.first)
        Assert.assertEquals(second, result.second)
    }

    @Test
    fun deleteInSameSetShouldTriggerRemovalOfRelatedChanges() {
        val uuid = "random"
        val first: MutableList<TransactionChange> = ArrayList()
        first.add(buildCreated().setUuid(uuid).build())
        first.add(buildDeleted().setUuid(uuid).build())
        val second: List<TransactionChange> = ArrayList()
        val result = syncDelegate.mergeChangeSets(first, second)
        Assert.assertEquals(1, result.first.size.toLong())
        Assert.assertEquals(result.first[0], first[1])
        Assert.assertEquals(0, result.second.size.toLong())
    }

    @Test
    fun deleteInDifferentSetShouldTriggerRemovalOfRelatedChanges() {
        val uuid = "random"
        val first: MutableList<TransactionChange> = ArrayList()
        first.add(buildUpdated().setUuid(uuid).build())
        val second: MutableList<TransactionChange> = ArrayList()
        second.add(buildDeleted().setUuid(uuid).build())
        val result = syncDelegate.mergeChangeSets(first, second)
        Assert.assertEquals(0, result.first.size.toLong())
        Assert.assertEquals(second, result.second)
    }

    @Test
    fun updatesShouldBeMerged() {
        val uuid = "random"
        val first: MutableList<TransactionChange> = ArrayList()
        first.add(buildUpdated().setUuid(uuid).build())
        first.add(buildUpdated().setUuid(uuid).build())
        val second: List<TransactionChange> = ArrayList()
        val result = syncDelegate.mergeChangeSets(first, second)
        Assert.assertEquals(1, result.first.size.toLong())
    }

    @Test
    fun insertCanBeMergedWithUpdate() {
        val uuid = "random"
        val first: MutableList<TransactionChange> = ArrayList()
        first.add(buildCreated().setUuid(uuid).build())
        first.add(buildUpdated().setUuid(uuid).build())
        val second: List<TransactionChange> = ArrayList()
        val result = syncDelegate.mergeChangeSets(first, second)
        Assert.assertEquals(1, result.first.size.toLong())
    }
}