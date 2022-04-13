package org.totschnig.myexpenses.sync

import android.content.ContentResolver
import org.mockito.Mockito
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.sync.json.TransactionChange

open class SyncAdapterBaseTest {
    private val currencyContext = Mockito.mock(CurrencyContext::class.java)
    private val repository: Repository = Repository(
        Mockito.mock(ContentResolver::class.java),
        currencyContext
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