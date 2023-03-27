package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.db2.Repository
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.sync.SyncDelegate
import org.totschnig.myexpenses.util.locale.HomeCurrencyProvider
import javax.inject.Singleton

@Module
class SyncModule {
    @Provides
    @Singleton
    fun provideSyncDelegate(
        currencyContext: CurrencyContext,
        featureManager: FeatureManager,
        repository: Repository,
        homeCurrencyProvider: HomeCurrencyProvider,
    ) = SyncDelegate(currencyContext, featureManager, repository, homeCurrencyProvider.homeCurrencyUnit)
}