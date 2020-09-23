package org.totschnig.myexpenses.di

import android.app.Activity
import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.ImageViewIntentProvider
import org.totschnig.myexpenses.activity.SystemImageViewIntentProvider
import org.totschnig.myexpenses.feature.Callback
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.ads.DefaultAdHandlerFactory
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import javax.inject.Named
import javax.inject.Singleton

@Module
class UiModule {
    @Provides
    @Singleton
    fun provideImageViewIntentProvider(): ImageViewIntentProvider = SystemImageViewIntentProvider()

    @Provides
    @Singleton
    fun provideAdHandlerFactory(application: MyApplication?, prefHandler: PrefHandler?, @Named(AppComponent.USER_COUNTRY) userCountry: String?): AdHandlerFactory = object : DefaultAdHandlerFactory(application, prefHandler, userCountry) {
        override fun isAdDisabled() = true
    }

    @Provides
    @Singleton
    fun provideFeatureManager(localeProvider: UserLocaleProvider): FeatureManager = try {
        Class.forName("org.totschnig.myexpenses.util.locale.PlatformSplitManager")
                .getConstructor(UserLocaleProvider::class.java)
                .newInstance(localeProvider) as FeatureManager
    } catch (e: Exception) {
        object : FeatureManager {
            var callback: Callback? = null
            override fun initApplication(application: Application) {
                //noop
            }

            override fun initActivity(activity: Activity) {
                //noop
            }

            override fun isFeatureInstalled(feature: FeatureManager.Feature) = true
            override fun requestFeature(feature: FeatureManager.Feature) {
                callback?.onAvailable()
            }

            override fun requestLocale(context: Context) {
                callback?.onAvailable()
            }

            override fun registerCallback(callback: Callback) {
                this.callback = callback
            }

            override fun unregister() {
                this.callback = null
            }
        }
    }
}