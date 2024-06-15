package org.totschnig.myexpenses

import kotlinx.coroutines.Dispatchers
import org.totschnig.myexpenses.di.*
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.testutils.MockLicenceModule
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.config.Configurator
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import java.time.Clock

//Used by Robolectric
class TestMyApplication : MyApplication() {
    override fun buildAppComponent(): AppComponent {
        return DaggerAppComponent.builder()
            .licenceModule(MockLicenceModule(Clock.systemUTC()))
            .applicationContext(this)
            .uiModule(object : UiModule() {
                override fun provideDiscoveryHelper(prefHandler: PrefHandler) =
                    IDiscoveryHelper.NO_OP

                override fun provideAdHandlerFactory(
                    application: MyApplication,
                    prefHandler: PrefHandler,
                    userCountry: String,
                    licenceHandler: LicenceHandler,
                    configurator: Configurator
                ) = object : AdHandlerFactory {}
            })
            .crashHandlerModule(object: CrashHandlerModule() {
                override fun providesCrashHandler(prefHandler: PrefHandler) = CrashHandler.NO_OP
            })
            .appmodule(object : AppModule() {
                override fun provideTracker() = NoOpTracker
            })
            .coroutineModule(object: CoroutineModule() {
                override fun provideCoroutineDispatcher() = Dispatchers.Main.immediate
            })
            .dataModule(DataModule(false))
            .configurationModule(object : ConfigurationModule() {
                override fun providesConfigurator(prefHandler: PrefHandler) = Configurator.NO_OP
            })
            .build()
    }
}