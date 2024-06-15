package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.config.Configurator
import javax.inject.Singleton

@Module
open class ConfigurationModule {
    @Provides
    @Singleton
    open fun providesConfigurator(prefHandler: PrefHandler): Configurator =
        try {
            Class.forName("org.totschnig.myexpenses.util.config.FirebaseRemoteConfigurator")
                .getConstructor().newInstance() as Configurator
        } catch (e: Exception) {
            Configurator.NO_OP
        }
}