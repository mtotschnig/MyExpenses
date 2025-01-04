package org.totschnig.myexpenses.di

import android.content.Context
import android.os.Bundle
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.PreferencesCurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.licence.LicenceStatus
import org.totschnig.myexpenses.util.tracking.Tracker
import timber.log.Timber
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

object NoOpTracker : Tracker {
    override fun init(context: Context, licenceStatus: LicenceStatus?) {
        //noop
    }

    override fun logEvent(eventName: String, params: Bundle?) {
        Timber.d("Event %s (%s)", eventName, params)
    }

    override fun setEnabled(enabled: Boolean) {
        //noop
    }
}

@Module
open class AppModule {
    @Provides
    @Singleton
    fun provideContext(myApplication: MyApplication): Context {
        return myApplication
    }

    @Provides
    @Singleton
    open fun provideTracker(): Tracker = try {
        Class.forName(
            "org.totschnig.myexpenses.util.tracking.PlatformTracker"
        ).newInstance() as Tracker
    } catch (_: Exception) {
        NoOpTracker
    }

    @Provides
    @Singleton
    @Named(AppComponent.USER_COUNTRY)
    fun provideUserCountry(application: MyApplication?) =
        (if (BuildConfig.DEBUG) null else Utils.getCountryFromTelephonyManager(application))
            ?: Locale.getDefault().country.takeIf { it.isNotEmpty() }?.lowercase(Locale.ROOT)
            ?: "us"

    @Provides
    @Singleton
    open fun provideCurrencyContext(
        prefHandler: PrefHandler,
        application: MyApplication
    ): CurrencyContext = PreferencesCurrencyContext(prefHandler, application)
}