package org.totschnig.myexpenses.feature

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.Utils
import java.util.*

sealed class Feature(@StringRes val labelResId: Int, val moduleName: String) {

    open fun canUninstall(context: Context, prefHandler: PrefHandler) = false

    companion object {
        fun fromModuleName(moduleName: String): Feature? =
            when (moduleName) {
                "ocr" -> OCR
                "webui" -> WEBUI
                "tesseract" -> TESSERACT
                "mlkit" -> MLKIT
                "mlkit_deva" -> DEVA
                "mlkit_han" -> HAN
                "mlkit_jpan" -> JPAN
                "mlkit_kore" -> KORE
                "mlkit_latn" -> LATN
                "drive" -> DRIVE
                "dropbox" -> DROPBOX
                "webdav" -> WEBDAV
                "fints" -> FINTS
                "sqlcrypt" -> SQLCRYPT
                else -> null
            }

        val values: List<Feature>
            get() = listOf(
                OCR,
                WEBUI,
                TESSERACT,
                MLKIT,
                DEVA,
                HAN,
                JPAN,
                KORE,
                LATN,
                DRIVE,
                DROPBOX,
                WEBDAV,
                SQLCRYPT,
                FINTS
            )

    }

    sealed class SyncBackend(labelResId: Int, moduleName: String) :
        Feature(labelResId, moduleName) {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            GenericAccountService.getAccountNames(context).none { account ->
                account.startsWith(
                    BackendService.entries.first { it.feature == this }.label
                )
            }
    }

    data object OCR : Feature(R.string.title_scan_receipt_feature, "ocr") {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            !prefHandler.getBoolean(PrefKey.OCR, false)
    }

    sealed class OcrEngine(labelResId: Int, moduleName: String) :
        Feature(labelResId, moduleName) {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            OCR.canUninstall(context, prefHandler) ||
                    getUserConfiguredOcrEngine(context, prefHandler) != this
    }

    sealed class MlkitProcessor(labelResId: Int, moduleName: String) :
        Feature(labelResId, moduleName) {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            OCR.canUninstall(context, prefHandler) ||
                    MLKIT.canUninstall(context, prefHandler) ||
                    getUserConfiguredMlkitScriptModule(context, prefHandler) != this
    }

    data object WEBUI : Feature(R.string.title_webui, "webui") {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            !prefHandler.getBoolean(PrefKey.UI_WEB, false)
    }

    data object TESSERACT : OcrEngine(R.string.title_tesseract, "tesseract")
    data object MLKIT : OcrEngine(R.string.title_mlkit, "mlkit")
    data object DEVA : MlkitProcessor(R.string.title_mlkit_deva, "mlkit_deva")
    data object HAN : MlkitProcessor(R.string.title_mlkit_han, "mlkit_han")
    data object JPAN : MlkitProcessor(R.string.title_mlkit_jpan, "mlkit_jpan")
    data object KORE : MlkitProcessor(R.string.title_mlkit_kore, "mlkit_kore")
    data object LATN : MlkitProcessor(R.string.title_mlkit_latn, "mlkit_latn")
    data object DRIVE : SyncBackend(R.string.title_drive, "drive")
    data object DROPBOX : SyncBackend(R.string.title_dropbox, "dropbox")
    data object WEBDAV : SyncBackend(R.string.title_webdav, "webdav")
    data object SQLCRYPT: Feature(R.string.title_sqlcrypt, "sqlcrypt") {
        override fun canUninstall(context: Context, prefHandler: PrefHandler): Boolean {
            return !prefHandler.encryptDatabase
        }
    }
    data object FINTS: Feature(R.string.title_fints, "fints")
}

enum class Script {
    Latn, Han, Deva, Jpan, Kore
}

fun getUserConfiguredOcrEngine(context: Context, prefHandler: PrefHandler) =
    prefHandler.getString(PrefKey.OCR_ENGINE, null)?.let { Feature.fromModuleName(it) }
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
    prefHandler.enumValueOrDefault(PrefKey.MLKIT_SCRIPT, defaultScript(context))

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
    return if ((localesForCountry?.size ?: 0) == 0) defaultLocale
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

    open fun requestLocale(language: String) {
        callback?.onLanguageAvailable(language)
    }

    open fun registerCallback(callback: Callback) {
        this.callback = callback
    }

    open fun unregister() {
        callback = null
    }

    open fun allowsUninstall() = false
    open fun installedFeatures(
        context: Context,
        prefHandler: PrefHandler,
        onlyUninstallable: Boolean = true
    ): Set<String> = emptySet()

    open fun installedLanguages(): Set<String> = emptySet()
    open fun uninstallFeatures(features: Set<String>) {}
    open fun uninstallLanguages(languages: Set<String>) {}
}

interface Callback {
    fun onLanguageAvailable(language: String) {}
    fun onFeatureAvailable(moduleNames: List<String>) {}
    fun onAsyncStartedFeature(feature: Feature) {}
    fun onAsyncStartedLanguage(displayLanguage: String) {}
    fun onError(throwable: Throwable) {}
}