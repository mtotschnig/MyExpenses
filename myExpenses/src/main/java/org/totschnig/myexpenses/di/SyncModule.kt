package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.sync.SyncDelegate

@Module
class SyncModule {
    @Provides
    fun provideSyncDelegate(
        currencyContext: CurrencyContext,
        featureManager: FeatureManager,
        repository: Repository
    ) = SyncDelegate(currencyContext, featureManager, repository, currencyContext.homeCurrencyUnit)
}