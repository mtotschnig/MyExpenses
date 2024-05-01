package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.config.Configurator
import javax.inject.Singleton

@Module
class ConfigurationModule {
    @Provides
    @Singleton
    open fun providesConfigurator(prefHandler: PrefHandler): Configurator = Configurator.NO_OP
}