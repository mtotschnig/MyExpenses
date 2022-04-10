package org.totschnig.myexpenses

import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.CrashHandlerModule
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.di.UiModule
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.testutils.MockLicenceModule
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.licence.LicenceHandler
import java.util.*

//Used by Robolectric
class TestMyApplication : MyApplication() {
    override fun buildAppComponent(systemLocale: Locale): AppComponent {
        return DaggerAppComponent.builder()
            .licenceModule(MockLicenceModule())
            .applicationContext(this)
            .systemLocale(systemLocale)
            .uiModule(object : UiModule() {
                override fun provideDiscoveryHelper(prefHandler: PrefHandler) =
                    IDiscoveryHelper.NO_OP

                override fun provideAdHandlerFactory(
                    application: MyApplication,
                    prefHandler: PrefHandler,
                    userCountry: String,
                    licenceHandler: LicenceHandler
                ) = object : AdHandlerFactory {}
            })
            .crashHandlerModule(object: CrashHandlerModule() {
                override fun providesCrashHandler(prefHandler: PrefHandler) = CrashHandler.NO_OP
            })
            .build()
    }

    override fun setupLogging() {}
}