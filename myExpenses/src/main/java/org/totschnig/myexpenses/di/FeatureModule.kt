package org.totschnig.myexpenses.di

import android.app.Activity
import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import org.totschnig.myexpenses.BuildConfig
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.feature.Callback
import org.totschnig.myexpenses.feature.FeatureManager
import org.totschnig.myexpenses.feature.OCR_MODULE
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import javax.inject.Singleton

@Module
class FeatureModule {
    private var ocrFeature: OcrFeature? = null

    @Provides
    fun provideOcrFeature(prefHandler: PrefHandler): OcrFeature? = requireOcrFeature(prefHandler)

    private fun requireOcrFeature(prefHandler: PrefHandler): OcrFeature? {
        if (ocrFeature != null) {
            return ocrFeature
        }
        return try {
            (Class.forName("org.totschnig.ocr.OcrFeatureImpl").getConstructor(PrefHandler::class.java)
                    .newInstance(prefHandler) as OcrFeature).also {
                ocrFeature = it
            }
        } catch (e: ClassNotFoundException) {
            CrashHandler.report(e)
            null
        }
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
                        requireOcrFeature(prefHandler)?.isAvailable(context) ?: false
                    } else
                        false

            override fun requestFeature(feature: String, activity: BaseActivity) {
                if (feature == OCR_MODULE) {
                    requireOcrFeature(activity.prefHandler)?.offerInstall(activity)
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