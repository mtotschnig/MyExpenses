package org.totschnig.myexpenses.di

import android.app.Activity
import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.activity.ImageViewIntentProvider
import org.totschnig.myexpenses.activity.SystemImageViewIntentProvider
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.feature.Callback
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.feature.OCR_MODULE
import org.totschnig.myexpenses.feature.OcrFeatureProvider
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.ads.AdHandlerFactory
import org.totschnig.myexpenses.util.ads.DefaultAdHandlerFactory
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
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
    fun provideOcrFeatureProvider(): OcrFeatureProvider = getOcrFeatureProvider()

    private fun getOcrFeatureProvider() = try {
        Class.forName("org.totschnig.ocr.OcrFeatureProviderImpl").kotlin.objectInstance as OcrFeatureProvider
    } catch (e: ClassNotFoundException) {
        CrashHandler.report(e)
        object : OcrFeatureProvider {}
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

            override fun isFeatureInstalled(feature: String, context: Context, prefHandler: PrefHandler) =
                    if (feature == OCR_MODULE) {
                        if (BuildConfig.FLAVOR_textRecognition == "intern") {
                            getOcrFeatureProvider().tessDataExists(context, prefHandler)
                        } else {
                            Utils.isIntentAvailable(context, OcrFeatureProvider.intent())
                        }
                    } else
                        false

            override fun requestFeature(feature: String, activity: BaseActivity) {
                if (feature == OCR_MODULE) {
                    if (BuildConfig.FLAVOR_textRecognition == "intern") {
                        activity.offerTessDataDownload()
                    } else {
                        MessageDialogFragment.newInstance(
                                null,
                                activity.getString(R.string.ocr_download_info),
                                MessageDialogFragment.Button(R.string.button_download, R.id.OCR_DOWNLOAD_COMMAND, null),
                                MessageDialogFragment.Button(R.string.learn_more, R.id.OCR_FAQ_COMMAND, null),
                                null).show(activity.getSupportFragmentManager(), "OCR_DOWNLOAD")
                    }
                }
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