package org.totschnig.myexpenses.feature

import android.app.Activity
import android.content.Context
import androidx.annotation.StringRes
import com.livefront.sealedenum.GenSealedEnum
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.preference.enumValueOrDefault
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import java.util.*

enum class Module(@StringRes val labelResId: Int) {
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
    WEBDAV(R.string.title_webdav),
    ONEDRIVE(R.string.title_onedrive),
    SQLCRYPT(R.string.title_sqlcrypt),
    FINTS(R.string.title_fints),
    JACKSON(R.string.title_jackson);

    val moduleName: String
        get() = name.lowercase(Locale.ROOT)

    companion object {
        fun from(moduleName: String) = valueOf(moduleName.uppercase())

        fun print(context: Context, moduleName: String) = try {
            context.getString(from(moduleName).labelResId)
        } catch (e: IllegalArgumentException) {
            CrashHandler.report(Throwable("Unknown module: $moduleName"))
            moduleName
        }
    }
}

sealed class Feature(vararg val requiredModules: Module) {

    val labelResId: Int = mainModule.labelResId

    val mainModule
        get() = requiredModules.first()

    open fun canUninstall(context: Context, prefHandler: PrefHandler) = false

    @GenSealedEnum
    companion object {
        fun dependentFeatures(moduleName: String) = Feature.values
            .filter {
                it.requiredModules.contains(Module.from(moduleName))
            }

    }

    sealed class SyncBackend(vararg requiredModules: Module) :
        Feature(*requiredModules) {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            GenericAccountService.getAccountNames(context).none { account ->
                account.startsWith(
                    BackendService.entries.first { it.feature == this }.label
                )
            }
    }

    data object OCR : Feature(Module.OCR) {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            !prefHandler.getBoolean(PrefKey.OCR, false)
    }

    sealed class OcrEngine(val engineClassName: String, vararg requiredModules: Module) :
        Feature(*requiredModules) {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            OCR.canUninstall(context, prefHandler) ||
                    getUserConfiguredOcrEngine(context, prefHandler) != this
    }

    sealed class MlkitProcessor(vararg requiredModules: Module) :
        Feature(*requiredModules) {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            OCR.canUninstall(context, prefHandler) ||
                    MLKIT.canUninstall(context, prefHandler) ||
                    getUserConfiguredMlkitScriptModule(context, prefHandler) != this
    }

    data object WEBUI : Feature(Module.WEBUI) {
        override fun canUninstall(context: Context, prefHandler: PrefHandler) =
            !prefHandler.getBoolean(PrefKey.UI_WEB, false)
    }

    data object TESSERACT : OcrEngine(
        "org.totschnig.tesseract.Engine",
        Module.TESSERACT
    )
    data object MLKIT : OcrEngine(
        "org.totschnig.mlkit.Engine",
        Module.MLKIT
    )
    data object DEVA : MlkitProcessor(Module.MLKIT_DEVA)
    data object HAN : MlkitProcessor(Module.MLKIT_HAN)
    data object JPAN : MlkitProcessor(Module.MLKIT_JPAN)
    data object KORE : MlkitProcessor(Module.MLKIT_KORE)
    data object LATN : MlkitProcessor(Module.MLKIT_LATN)
    data object DRIVE : SyncBackend(Module.DRIVE)
    data object DROPBOX : SyncBackend(Module.DROPBOX, Module.JACKSON)
    data object WEBDAV : SyncBackend(Module.WEBDAV)
    data object ONEDRIVE : SyncBackend(Module.ONEDRIVE, Module.JACKSON)
    data object SQLCRYPT: Feature(Module.SQLCRYPT) {
        override fun canUninstall(context: Context, prefHandler: PrefHandler): Boolean {
            return !prefHandler.encryptDatabase
        }
    }
    data object FINTS: Feature(Module.FINTS)
}

enum class Script {
    Latn, Han, Deva, Jpan, Kore
}

fun getUserConfiguredOcrEngine(context: Context, prefHandler: PrefHandler): Feature.OcrEngine =
    prefHandler.getString(PrefKey.OCR_ENGINE, null)?.let {
        when(it) {
            "tesseract" -> Feature.TESSERACT
            "mlkit" -> Feature.MLKIT
            else -> null
        }
    }
        ?: getDefaultOcrEngine(context)

fun getUserConfiguredMlkitScriptModule(context: Context, prefHandler: PrefHandler): Feature =
    when( getUserConfiguredMlkitScript(context, prefHandler)) {
        Script.Latn -> Feature.LATN
        Script.Han -> Feature.HAN
        Script.Deva -> Feature.DEVA
        Script.Jpan -> Feature.JPAN
        Script.Kore -> Feature.KORE
    }

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
fun getDefaultOcrEngine(context: Context): Feature.OcrEngine =
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
    open fun installedModules(
        context: Context,
        prefHandler: PrefHandler,
        onlyUninstallable: Boolean = true
    ): Set<String> = emptySet()

    open fun installedLanguages(): Set<String> = emptySet()
    open fun uninstallModules(features: Set<String>) {}
    open fun uninstallLanguages(languages: Set<String>) {}
}

interface Callback {
    fun onLanguageAvailable(language: String) {}
    fun onFeatureAvailable(moduleNames: List<String>) {}
    fun onAsyncStartedFeature(feature: Feature) {}
    fun onAsyncStartedLanguage(displayLanguage: String) {}
    fun onError(throwable: Throwable) {}
}