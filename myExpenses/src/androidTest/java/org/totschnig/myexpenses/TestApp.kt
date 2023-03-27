package org.totschnig.myexpenses

import android.content.Context
import androidx.core.os.ConfigurationCompat
import androidx.test.platform.app.InstrumentationRegistry
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.AppModule
import org.totschnig.myexpenses.di.CrashHandlerModule
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.di.UiModule
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.testutils.Fixture
import org.totschnig.myexpenses.testutils.MockLicenceModule
import org.totschnig.myexpenses.testutils.TestCoroutineModule
import org.totschnig.myexpenses.testutils.TestDataModule
import org.totschnig.myexpenses.testutils.TestFeatureModule
import org.totschnig.myexpenses.testutils.TestViewModelModule
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.locale.HomeCurrencyProvider
import org.totschnig.myexpenses.util.locale.HomeCurrencyProviderImpl
import java.util.*

class TestApp : MyApplication() {
    lateinit var fixture: Fixture
    override fun onCreate() {
        super.onCreate()
        fixture = Fixture(InstrumentationRegistry.getInstrumentation())
    }

    override fun buildAppComponent(): AppComponent = DaggerAppComponent.builder()
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
        .appmodule(object : AppModule() {
            override fun provideHomeCurrencyProvider(
                prefHandler: PrefHandler,
                context: Context,
                currencyContext: CurrencyContext
            ) = object : HomeCurrencyProviderImpl(prefHandler, context, currencyContext) {
                override val localCurrency: Currency
                    get() {
                        val locale =
                            ConfigurationCompat.getLocales(context.resources.configuration)
                                .get(0)!!
                        return if (locale.country == "VI") Currency.getInstance("VND") else
                            Currency.getInstance(locale)
                    }
            }
        })
        .build()

    override fun enableStrictMode() {}
}