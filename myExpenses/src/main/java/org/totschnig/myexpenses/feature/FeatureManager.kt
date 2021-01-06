package org.totschnig.myexpenses.feature

import android.app.Activity
import android.content.Context
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import java.util.*

const val OCR_MODULE = "ocr"
const val ENGINE_TESSERACT = "tesseract"
const val ENGINE_MLKIT = "mlkit"

/**
 * check if language has non-latin script and is supported by Tesseract
 */
fun getDefaultEngine(context: Context) = if (getLocaleForUserCountry(context).language in arrayOf(
                "am", "ar", "as", "be", "bn", "bo", "bg", "zh", "dz", "el", "fa", "gu", "iw", "hi",
                "iu", "jv", "kn", "ka", "kk", "km", "ky", "ko", "lo", "ml", "mn", "my", "ne", "or",
                "pa", "ps", "ru", "si", "sd", "sr", "ta", "te", "tg", "th", "ti", "ug", "uk", "ur"))
    ENGINE_TESSERACT else ENGINE_MLKIT

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
    val ocrFeature: OcrFeature?
        get() = application.appComponent.ocrFeature()

    open fun initApplication(application: MyApplication) {
        this.application = application
    }
    open fun initActivity(activity: Activity) {}
    open fun isFeatureInstalled(feature: String, context: Context) =
            if (feature == OCR_MODULE) {
                ocrFeature?.isAvailable(context) ?: false
            } else
                false

    open fun requestFeature(feature: String, activity: BaseActivity) {
        if (feature == OCR_MODULE) {
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
    fun onFeatureAvailable() {}
    fun onAsyncStartedFeature(feature: String) {}
    fun onAsyncStartedLanguage(displayLanguage: String) {}
    fun onError(throwable: Throwable) {}
}