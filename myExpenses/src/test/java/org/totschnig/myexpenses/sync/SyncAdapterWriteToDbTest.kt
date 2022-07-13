package org.totschnig.myexpenses.sync

import android.content.ContentProviderOperation
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.CurrencyFormatter

@RunWith(RobolectricTestRunner::class)
class SyncAdapterWriteToDbTest {
    private lateinit var syncDelegate: SyncDelegate
    private lateinit var ops: ArrayList<ContentProviderOperation>

    @Before
    fun setup() {
        ops = ArrayList()
    }

    private fun setupSync() {
        syncDelegate = SyncDelegate(currencyContext, featureManager, repository)
        syncDelegate.account = Account()
    }

    private fun setupSyncWithFakeResolver() {
        syncDelegate = SyncDelegate(currencyContext, featureManager, repository) { _, _ -> 1 }
        syncDelegate.account = Account()
    }

    private val currencyContext = Mockito.mock(CurrencyContext::class.java)
    private val featureManager = Mockito.mock(FeatureManager::class.java)
    private val repository = Repository(
        ApplicationProvider.getApplicationContext<MyApplication>(),
        currencyContext,
        Mockito.mock(CurrencyFormatter::class.java),
        Mockito.mock(PrefHandler::class.java)
    )

    @Test
    fun createdChangeShouldBeCollectedAsInsertOperation() {
        setupSync()
        val change = TransactionChange.builder()
                .setType(TransactionChange.Type.created)
                .setUuid("any")
                .setCurrentTimeStamp()
                .setAmount(123L)
                .build()
        syncDelegate.collectOperations(change, ops, -1)
        Assert.assertEquals(1, ops.size.toLong())
        Assert.assertTrue(ops[0].isInsert)
    }

    @Test
    fun updatedChangeShouldBeCollectedAsUpdateOperationWithoutTag() {
        setupSyncWithFakeResolver()
        val change = TransactionChange.builder()
                .setType(TransactionChange.Type.updated)
                .setUuid("any")
                .setCurrentTimeStamp()
                .setAmount(123L)
                .build()
        syncDelegate.collectOperations(change, ops, -1)
        Assert.assertEquals(2, ops.size.toLong())
        Assert.assertTrue(ops[0].isUpdate)
        Assert.assertTrue(ops[1].isDelete)
        Assert.assertEquals(TransactionProvider.TRANSACTIONS_TAGS_URI, ops[1].uri)
    }

    @Test
    fun createChangeWithTag() {
        setupSync()
        val change = TransactionChange.builder()
                .setType(TransactionChange.Type.created)
                .setUuid("any")
                .setCurrentTimeStamp()
                .setAmount(123L)
                .setTags(listOf("tag"))
                .build()
        syncDelegate.collectOperations(change, ops, -1)
        Assert.assertEquals(2, ops.size.toLong())
        Assert.assertTrue(ops[0].isInsert)
        Assert.assertEquals(TransactionProvider.TRANSACTIONS_TAGS_URI, ops[1].uri)
        Assert.assertTrue(ops[1].isInsert)
    }

    @Test
    fun deletedChangeShouldBeCollectedAsDeleteOperation() {
        setupSyncWithFakeResolver()
        val change = TransactionChange.builder()
                .setType(TransactionChange.Type.deleted)
                .setUuid("any")
                .setCurrentTimeStamp()
                .build()
        syncDelegate.collectOperations(change, ops, -1)
        Assert.assertEquals(1, ops.size.toLong())
        Assert.assertTrue(ops[0].isDelete)
    }
}