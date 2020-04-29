package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.viewmodel.AbstractSyncBackendViewModel
import org.totschnig.myexpenses.viewmodel.SyncBackendViewModel

@Module
open class ViewModelModule {
    @Provides
    open fun provideSyncBackendViewModelClass(): Class<out AbstractSyncBackendViewModel> = SyncBackendViewModel::class.java
}