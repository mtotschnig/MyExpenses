package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.crashreporting.AcraCrashHandler
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import javax.inject.Singleton

@Module
open class CrashHandlerModule {
    @Provides
    @Singleton
    open fun providesCrashHandler(prefHandler: PrefHandler): CrashHandler {
        return AcraCrashHandler()
    }
}