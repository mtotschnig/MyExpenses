package org.totschnig.myexpenses.di

import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.ImageViewIntentProvider
import org.totschnig.myexpenses.activity.SystemImageViewIntentProvider
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.ads.DefaultAdHandlerFactory
import org.totschnig.myexpenses.util.bundle.LocaleManager
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
    fun provideLanguageManager(): LocaleManager {
        return object : LocaleManager {}
    }
}