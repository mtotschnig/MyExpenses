package org.totschnig.myexpenses

import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.di.UiModule
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.testutils.MockLicenceModule
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import java.util.*

class TestMyApplication : MyApplication() {
    override fun buildAppComponent(systemLocale: Locale): AppComponent {
        return DaggerAppComponent.builder()
                .licenceModule(MockLicenceModule())
                .applicationContext(this)
                .systemLocale(systemLocale)
                .uiModule(object : UiModule() {
                    override fun provideDiscoveryHelper(prefHandler: PrefHandler) =
                            IDiscoveryHelper.NO_OP
                })
                .build()
    }

    override fun setupLogging() {}
}