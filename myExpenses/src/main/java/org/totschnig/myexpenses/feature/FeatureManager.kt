package org.totschnig.myexpenses.feature

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import java.util.*

enum class Feature(@StringRes val labelResId: Int) {
    OCR(R.string.title_scan_receipt_feature), WEBUI(R.string.title_webui), TESSERACT(R.string.title_tesseract), MLKIT(R.string.title_mlkit);
    val moduleName
        get() = name.toLowerCase(Locale.ROOT)
    companion object {
        fun fromModuleName(moduleName: String?) = try {
            moduleName?.let { valueOf(it.toUpperCase(Locale.ROOT)) }
        } catch (e: IllegalArgumentException) {
            null
        }
    }
}

fun getUserConfiguredOcrEngine(context: Context, prefHandler: PrefHandler) =
        Feature.fromModuleName(prefHandler.getString(PrefKey.OCR_ENGINE, null)) ?: getDefaultOcrEngine(context)

/**
 * check if language has non-latin script and is supported by Tesseract
 */
fun getDefaultOcrEngine(context: Context) = if (getLocaleForUserCountry(context).language in arrayOf(
                "am", "ar", "as", "be", "bn", "bo", "bg", "zh", "dz", "el", "fa", "gu", "iw", "hi",
                "iu", "jv", "kn", "ka", "kk", "km", "ky", "ko", "lo", "ml", "mn", "my", "ne", "or",
                "pa", "ps", "ru", "si", "sd", "sr", "ta", "te", "tg", "th", "ti", "ug", "uk", "ur"))
    Feature.TESSERACT else Feature.MLKIT

fun getLocaleForUserCountry(context: Context) =
        getLocaleForUserCountry(Utils.getCountryFromTelephonyManager(context))

fun getLocaleForUserCountry(country: String?) = getLocaleForUserCountry(country, Locale.getDefault())

fun getLocaleForUserCountry(country: String?, defaultLocale: Locale): Locale {
    val localesForCountry = country?.toUpperCase(Locale.ROOT)?.let {
        Locale.getAvailableLocales().filter { locale -> it == locale.country }
    }
    return if (localesForCountry?.size ?: 0 == 0) defaultLocale
    else localesForCountry!!.find { locale -> locale.language == defaultLocale.language }
            ?: localesForCountry[0]
}

abstract class FeatureManager {
    lateinit var application: MyApplication
    var callback: Callback? = null
    private val ocrFeature: OcrFeature?
        get() = application.appComponent.ocrFeature()

    open fun initApplication(application: MyApplication) {
        this.application = application
    }
    open fun initActivity(activity: Activity) {}
    open fun isFeatureInstalled(feature: Feature, context: Context) =
            if (feature == Feature.OCR) {
                ocrFeature?.isAvailable(context) ?: false
            } else
                true

    open fun requestFeature(feature: Feature, activity: BaseActivity) {
        if (feature == Feature.OCR) {
            ocrFeature?.offerInstall(activity)
        }
    }
    open fun requestLocale(context: Context) {
        callback?.onLanguageAvailable()
    }

    open fun registerCallback(callback: Callback) {
        this.callback = callback
    }

    open fun unregister() {
        callback = null
    }

    open fun allowsUninstall() = false
    open fun installedFeatures(): Set<String> = emptySet()
    open fun installedLanguages(): Set<String> = emptySet()
    open fun uninstallFeatures(features: Set<String>) {}
    open fun uninstallLanguages(languages: Set<String>) {}
}

interface Callback {
    fun onLanguageAvailable() {}
    fun onFeatureAvailable(moduleNames: List<String>) {}
    fun onAsyncStartedFeature(feature: Feature) {}
    fun onAsyncStartedLanguage(displayLanguage: String) {}
    fun onError(throwable: Throwable) {}
}