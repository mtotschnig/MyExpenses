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

    fun buildCreated(uuid: String) = buildWithTimestamp(uuid,TransactionChange.Type.created)

    fun buildDeleted(uuid: String) = buildWithTimestamp(uuid,TransactionChange.Type.deleted)

    fun buildUpdated(uuid: String) = buildWithTimestamp(uuid,TransactionChange.Type.updated)

    fun buildSpecial(uuid: String) = buildWithTimestamp(uuid,TransactionChange.Type.unsplit)


    private fun buildWithTimestamp(uuid: String, type: TransactionChange.Type) =
        TransactionChange(
            uuid = uuid, type = type, timeStamp = TransactionChange.currentTimStamp
        )
}