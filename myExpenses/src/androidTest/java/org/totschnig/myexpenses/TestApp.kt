package org.totschnig.myexpenses

import androidx.test.platform.app.InstrumentationRegistry
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.CrashHandlerModule
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.di.UiModule
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.testutils.*
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.util.*

class TestApp : MyApplication() {
    lateinit var fixture: Fixture
    override fun onCreate() {
        super.onCreate()
        fixture = Fixture(InstrumentationRegistry.getInstrumentation())
    }

    override fun buildAppComponent(systemLocale: Locale): AppComponent = DaggerAppComponent.builder()
        .coroutineModule(TestCoroutineModule)
        .viewModelModule(TestViewModelModule)
        .dataModule(TestDataModule)
        .featureModule(TestFeatureModule)
        .crashHandlerModule(object : CrashHandlerModule() {
            override fun providesCrashHandler(prefHandler: PrefHandler) = CrashHandler.NO_OP
        })
        .uiModule(object : UiModule() {
            override fun provideDiscoveryHelper(prefHandler: PrefHandler) =
                IDiscoveryHelper.NO_OP
        })
        .licenceModule(MockLicenceModule())
        .applicationContext(this)
        .systemLocale(systemLocale)
        .build()

    override fun enableStrictMode() {}
}