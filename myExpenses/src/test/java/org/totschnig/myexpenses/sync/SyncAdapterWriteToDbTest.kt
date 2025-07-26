package org.totschnig.myexpenses.sync

import android.content.ContentProviderOperation
import com.google.common.truth.Truth.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.sync.json.TransactionChange
import java.util.*

@RunWith(RobolectricTestRunner::class)
class SyncAdapterWriteToDbTest: BaseTestWithRepository() {
    private lateinit var syncDelegate: SyncDelegate
    private lateinit var ops: ArrayList<ContentProviderOperation>

    @Before
    fun setup() {
        ops = ArrayList()
    }

    private fun setupSync() {
        syncDelegate = SyncDelegate(currencyContext, featureManager, repository, homeCurrency)
        syncDelegate.account = Account(label = "", currency = "EUR", type = AccountType.CASH)
    }

    private fun setupSyncWithFakeResolver() {
        syncDelegate = SyncDelegate(currencyContext, featureManager, repository, homeCurrency) { _, _ -> 1 }
        syncDelegate.account = Account(label = "", currency = "EUR", type = AccountType.CASH)
    }

    private val featureManager = Mockito.mock(FeatureManager::class.java)
    private val homeCurrency = CurrencyUnit.DebugInstance

    @Test
    fun createdChangeShouldBeCollectedAsInsertOperation() {
        setupSync()
        val change = TransactionChange.builder()
                .setType(TransactionChange.Type.created)
                .setUuid("any")
                .setCurrentTimeStamp()
                .setAmount(123L)
                .build()
        syncDelegate.collectOperations(change, ops)
        assertThat(ops).hasSize(1)
        assertThat(ops[0].isInsert).isTrue()
    }

    @Test
    fun createChangeWithTag() {
        setupSync()
        val change = TransactionChange.builder()
                .setType(TransactionChange.Type.created)
                .setUuid("any")
                .setCurrentTimeStamp()
                .setAmount(123L)
                .setTags(setOf("tag"))
                .build()
        syncDelegate.collectOperations(change, ops)
        assertThat(ops).hasSize(2)
        assertThat(ops[0].isInsert).isTrue()
        assertThat(ops[1].uri.path).isEqualTo("/transactions/tags")
        assertThat(ops[1].isInsert).isTrue()
    }

    @Test
    fun deletedChangeShouldBeCollectedAsDeleteOperation() {
        setupSyncWithFakeResolver()
        val change = TransactionChange.builder()
                .setType(TransactionChange.Type.deleted)
                .setUuid("any")
                .setCurrentTimeStamp()
                .build()
        syncDelegate.collectOperations(change, ops)
        assertThat(ops).hasSize(1)
        assertThat(ops[0].isDelete).isTrue()
    }
}