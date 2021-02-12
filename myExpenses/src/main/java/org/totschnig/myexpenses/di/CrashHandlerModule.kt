package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.util.crashreporting.AcraCrashHandler
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import javax.inject.Singleton

@Module
open class CrashHandlerModule {
    @Provides
    @Singleton
    open fun providesCrashHandler(): CrashHandler {
        return AcraCrashHandler()
    }
}