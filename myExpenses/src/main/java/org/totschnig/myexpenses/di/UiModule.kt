package org.totschnig.myexpenses.di

import android.content.Context
import androidx.fragment.app.FragmentActivity
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.ImageViewIntentProvider
import org.totschnig.myexpenses.activity.SystemImageViewIntentProvider
import org.totschnig.myexpenses.dialog.RemindRateDialogFragment
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.ui.DiscoveryHelper
import org.totschnig.myexpenses.ui.IDiscoveryHelper
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.distrib.ReviewManager
import org.totschnig.myexpenses.util.licence.LicenceHandler
import javax.inject.Named
import javax.inject.Singleton

@Module
open class UiModule {
    @Provides
    @Singleton
    fun provideImageViewIntentProvider(): ImageViewIntentProvider = SystemImageViewIntentProvider()

    @Provides
    @Singleton
    fun provideAdHandlerFactory(application: MyApplication, prefHandler: PrefHandler, @Named(AppComponent.USER_COUNTRY) userCountry: String, licenceHandler: LicenceHandler): AdHandlerFactory =
           try {
               Class.forName("org.totschnig.myexpenses.util.ads.PlatformAdHandlerFactory")
                       .getConstructor(Context::class.java, PrefHandler::class.java, String::class.java, LicenceHandler::class.java)
                       .newInstance(application, prefHandler, userCountry, licenceHandler) as AdHandlerFactory
           } catch (e: Exception) {
               object : AdHandlerFactory {}
    }

    @Provides
    @Singleton
    open fun provideDiscoveryHelper(prefHandler: PrefHandler): IDiscoveryHelper = DiscoveryHelper(prefHandler)

    @Provides
    @Singleton
    open fun provideReviewManager(prefHandler: PrefHandler): ReviewManager = try {
        Class.forName("org.totschnig.myexpenses.util.distrib.PlatformReviewManager")
                .getConstructor(PrefHandler::class.java)
                .newInstance(prefHandler) as ReviewManager
    } catch (e: Exception) {
        object : ReviewManager {
            override fun onEditTransactionResult(activity: FragmentActivity) {
                RemindRateDialogFragment.maybeShow(prefHandler, activity)
            }
        }
    }

}