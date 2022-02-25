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
import org.totschnig.myexpenses.util.enumValueOrDefault
import org.totschnig.myexpenses.util.enumValueOrNull
import java.util.*

enum class Feature(@StringRes val labelResId: Int) {
    OCR(R.string.title_scan_receipt_feature),
    WEBUI(R.string.title_webui),
    TESSERACT(R.string.title_tesseract),
    MLKIT(R.string.title_mlkit),
    MLKIT_DEVA(R.string.title_mlkit_deva),
    MLKIT_HAN(R.string.title_mlkit_han),
    MLKIT_JPAN(R.string.title_mlkit_jpan),
    MLKIT_KORE(R.string.title_mlkit_kore),
    MLKIT_LATN(R.string.title_mlkit_latn),
    DRIVE(R.string.title_drive),
    DROPBOX(R.string.title_dropbox),
    WEBDAV(R.string.title_webdav)
    ;

    val moduleName
        get() = name.lowercase(Locale.ROOT)

    companion object {
        fun fromModuleName(moduleName: String?): Feature? =
            enumValueOrNull<Feature>(moduleName?.uppercase(Locale.ROOT))
    }
}

enum class Script {
    Latn, Han, Deva, Jpan, Kore
}

fun getUserConfiguredOcrEngine(context: Context, prefHandler: PrefHandler) =
    Feature.fromModuleName(prefHandler.getString(PrefKey.OCR_ENGINE, null))
        ?: getDefaultOcrEngine(context)

fun getUserConfiguredMlkitScriptModule(context: Context, prefHandler: PrefHandler) =
    Feature.fromModuleName(
        "mlkit_${
            getUserConfiguredMlkitScript(context, prefHandler).name.lowercase(
                Locale.ROOT
            )
        }"
    )!!

fun getUserConfiguredMlkitScript(context: Context, prefHandler: PrefHandler) =
    enumValueOrDefault(prefHandler.getString(PrefKey.MLKIT_SCRIPT, null), defaultScript(context))

private fun defaultScript(context: Context) =
    when (getLocaleForUserCountry(context).language) {
        "zh" -> Script.Han
        "hi", "mr", "ne" -> Script.Deva
        "ja" -> Script.Jpan
        "ko" -> Script.Kore
        else -> Script.Latn
    }

/**
 * check if language is unsupported by MlKit, but supported by Tesseract
 */
fun getDefaultOcrEngine(context: Context) =
    if (getLocaleForUserCountry(context).language in arrayOf(
            "am", "ar", "as", "be", "bn", "bo", "bg", "dz", "el", "fa", "gu", "iw",
            "iu", "jv", "kn", "ka", "kk", "km", "ky", "lo", "ml", "mn", "my", "ne", "or",
            "pa", "ps", "ru", "si", "sd", "sr", "ta", "te", "tg", "th", "ti", "ug", "uk", "ur"
        )
    )
        Feature.TESSERACT else Feature.MLKIT

fun getLocaleForUserCountry(context: Context) =
    getLocaleForUserCountry(Utils.getCountryFromTelephonyManager(context))

fun getLocaleForUserCountry(country: String?) =
    getLocaleForUserCountry(country, Locale.getDefault())

fun getLocaleForUserCountry(country: String?, defaultLocale: Locale): Locale {
    val localesForCountry = country?.uppercase(Locale.ROOT)?.let {
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

    open fun requestFeature(feature: Feature, context: Context) {
        if (feature == Feature.OCR) {
            (context as? BaseActivity)?.let { ocrFeature?.offerInstall(it) }
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