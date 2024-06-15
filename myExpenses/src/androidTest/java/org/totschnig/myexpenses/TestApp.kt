package org.totschnig.myexpenses

import androidx.core.os.ConfigurationCompat
import androidx.test.platform.app.InstrumentationRegistry
import org.totschnig.myexpenses.di.AppComponent
import org.totschnig.myexpenses.di.AppModule
import org.totschnig.myexpenses.di.CrashHandlerModule
import org.totschnig.myexpenses.di.DaggerAppComponent
import org.totschnig.myexpenses.di.UiModule
import org.totschnig.myexpenses.model.PreferencesCurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.testutils.Fixture
import org.totschnig.myexpenses.testutils.MockLicenceModule
import org.totschnig.myexpenses.testutils.TestCoroutineModule
import org.totschnig.myexpenses.testutils.TestDataModule
import org.totschnig.myexpenses.testutils.TestFeatureModule
import org.totschnig.myexpenses.testutils.TestViewModelModule
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.TemporalAmount
import java.util.Currency


class TestApp : MyApplication() {
    lateinit var fixture: Fixture

    var currentTime: Instant = Instant.EPOCH
    private val clock: Clock = object : Clock() {
        override fun getZone(): ZoneId {
            return ZoneId.systemDefault()
        }

        override fun withZone(zone: ZoneId?): Clock {
            return this
        }

        override fun instant(): Instant {
            return currentTime
        }

    }

    fun advanceClock(byAmount: TemporalAmount) {
        currentTime += byAmount
    }

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
        .licenceModule(MockLicenceModule(clock))
        .applicationContext(this)
        .appmodule(object : AppModule() {
            override fun provideCurrencyContext(
                prefHandler: PrefHandler,
                application: MyApplication
            ) = object : PreferencesCurrencyContext(prefHandler, application) {
                override val localCurrency: Currency
                    get() {
                        val locale =
                            ConfigurationCompat.getLocales(application.resources.configuration)
                                .get(0)!!
                        return if (locale.country == "VI") Currency.getInstance("VND") else
                            Currency.getInstance(locale)
                    }
            }
        })
        .build()

    override fun enableStrictMode() {}
}