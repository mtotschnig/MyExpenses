package org.totschnig.myexpenses.sync

import androidx.test.core.app.ApplicationProvider
import org.mockito.Mockito
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.sync.json.TransactionChange
import org.totschnig.myexpenses.util.CurrencyFormatter

open class SyncAdapterBaseTest {
    private val currencyContext = Mockito.mock(CurrencyContext::class.java)
    private val repository: Repository = Repository(
        ApplicationProvider.getApplicationContext<MyApplication>(),
        currencyContext,
        Mockito.mock(CurrencyFormatter::class.java),
        Mockito.mock(PrefHandler::class.java)
    )
    val syncDelegate = SyncDelegate(
        currencyContext, Mockito.mock(
            FeatureManager::class.java
        ), repository
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