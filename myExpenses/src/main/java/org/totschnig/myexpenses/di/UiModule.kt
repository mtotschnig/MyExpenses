package org.totschnig.myexpenses.di

import android.content.Context
import androidx.fragment.app.FragmentActivity
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.ViewIntentProvider
import org.totschnig.myexpenses.activity.SystemViewIntentProvider
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.CurrencyFormatter
import org.totschnig.myexpenses.util.ICurrencyFormatter
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.config.Configurator
import org.totschnig.myexpenses.util.distrib.ReviewManager
import org.totschnig.myexpenses.util.licence.LicenceHandler
import javax.inject.Named
import javax.inject.Singleton

@Module
open class UiModule {
    @Provides
    @Singleton
    fun provideImageViewIntentProvider(): ViewIntentProvider = SystemViewIntentProvider()

    @Provides
    @Singleton
    open fun provideAdHandlerFactory(
        application: MyApplication,
        prefHandler: PrefHandler,
        @Named(AppComponent.USER_COUNTRY) userCountry: String,
        licenceHandler: LicenceHandler,
        configurator: Configurator
    ): AdHandlerFactory =
        try {
            Class.forName("org.totschnig.myexpenses.util.ads.PlatformAdHandlerFactory")
                .getConstructor(
                    Context::class.java,
                    PrefHandler::class.java,
                    String::class.java,
                    LicenceHandler::class.java,
                    Configurator::class.java
                )
                .newInstance(
                    application,
                    prefHandler,
                    userCountry,
                    licenceHandler,
                    configurator
                ) as AdHandlerFactory
        } catch (_: Exception) {
            object : AdHandlerFactory {}
        }

    @Provides
    @Singleton
    open fun provideDiscoveryHelper(prefHandler: PrefHandler): IDiscoveryHelper =
        DiscoveryHelper(prefHandler)

    @Provides
    @Singleton
    open fun provideReviewManager(prefHandler: PrefHandler): ReviewManager = try {
        Class.forName("org.totschnig.myexpenses.util.distrib.PlatformReviewManager")
            .getConstructor(PrefHandler::class.java)
            .newInstance(prefHandler) as ReviewManager
    } catch (_: Exception) {
        object : ReviewManager {
            override fun onEditTransactionResult(activity: FragmentActivity) {
                RemindRateDialogFragment.maybeShow(prefHandler, activity)
            }
        }
    }

    @Provides
    @Singleton
    fun providesCurrencyFormatter(
        application: MyApplication,
        prefHandler: PrefHandler
    ): ICurrencyFormatter = CurrencyFormatter(prefHandler, application)

}