package org.totschnig.myexpenses

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.AppModule
import org.totschnig.myexpenses.di.CrashHandlerModule
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.di.UiModule
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.testutils.Fixture
import org.totschnig.myexpenses.testutils.MockLicenceModule
import org.totschnig.myexpenses.testutils.TestCoroutineModule
import org.totschnig.myexpenses.testutils.TestDataModule
import org.totschnig.myexpenses.testutils.TestFeatureModule
import org.totschnig.myexpenses.testutils.TestViewModelModule
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import org.totschnig.myexpenses.util.locale.UserLocaleProviderImpl
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
        .appmodule(object : AppModule() {
            override fun provideUserLocaleProvider(
                prefHandler: PrefHandler,
                locale: Locale
            ): UserLocaleProvider {
                return object: UserLocaleProviderImpl(prefHandler, locale) {
                    override fun wrapContext(context: Context) = context
                    override fun getLocalCurrency(context: Context) = Utils.getSaveDefault()
                }
            }
        })
        .build()

    override fun enableStrictMode() {}
}