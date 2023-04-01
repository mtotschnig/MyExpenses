package org.totschnig.myexpenses.sync

import org.mockito.Mockito
import org.totschnig.myexpenses.BaseTestWithRepository
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.sync.json.TransactionChange

open class SyncAdapterBaseTest: BaseTestWithRepository() {
    val syncDelegate = SyncDelegate(
        currencyContext = currencyContext,
        featureManager = Mockito.mock(FeatureManager::class.java),
        repository = repository,
        homeCurrency = CurrencyUnit.DebugInstance
    )

    fun buildCreated(): TransactionChange.Builder {
        return buildWithTimestamp().setType(TransactionChange.Type.created)
    }

    private fun buildWithTimestamp(): TransactionChange.Builder {
        return TransactionChange.builder().setCurrentTimeStamp()
    }

    fun buildDeleted(): TransactionChange.Builder {
        return buildWithTimestamp().setType(TransactionChange.Type.deleted)
    }

    fun buildUpdated(): TransactionChange.Builder {
        return buildWithTimestamp().setType(TransactionChange.Type.updated)
    }
}