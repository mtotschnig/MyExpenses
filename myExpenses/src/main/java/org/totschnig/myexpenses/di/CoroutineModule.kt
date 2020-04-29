package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Module
open class CoroutineModule {
    @Provides
    open fun provideCoroutineDispatcher(): CoroutineDispatcher = Dispatchers.IO
}