package org.totschnig.myexpenses

import android.content.Context
import androidx.core.os.ConfigurationCompat
import androidx.test.platform.app.InstrumentationRegistry
import org.totschnig.myexpenses.di.*
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.testutils.*
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
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