package org.totschnig.ocr

import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.io.File
import java.util.*
import javax.inject.Inject

const val ENGINE_TESSERACT = "tesseract"
const val ENGINE_MLKIT = "mlkit"

class OcrHandlerImpl @Inject constructor(prefHandler: PrefHandler, userLocaleProvider: UserLocaleProvider, context: Context) : AbstractOcrHandlerImpl(prefHandler, userLocaleProvider, context) {
    override suspend fun runTextRecognition(file: File, context: Context) =
            getEngine(context, prefHandler)?.let { processTextRecognitionResult(it.run(file, context, prefHandler), queryPayees()) } ?: throw java.lang.IllegalStateException("No engine loaded")

    override suspend fun handleData(intent: Intent): OcrResult {
        throw IllegalStateException()
    }

    override fun info(context: Context): CharSequence? {
       return getEngine(context, prefHandler)?.info(context, prefHandler)
    }

    companion object {
        fun getEngine(context: Context, prefHandler: PrefHandler) = getEngine(
                prefHandler.getString(PrefKey.OCR_ENGINE, null) ?: getDefaultEngine(getLocaleForUserCountry(context)))

        fun getEngine(engine: String) = try {
            Class.forName("org.totschnig.$engine.Engine").kotlin.objectInstance as Engine
        } catch (e: Exception) {
            null
        }

        /**
         * check if language has non-latin script and is supported by Tesseract
         */
        private fun getDefaultEngine(locale: Locale) = if (locale.language in arrayOf(
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
            return if(localesForCountry?.size ?: 0 == 0) defaultLocale
            else localesForCountry!!.find { locale -> locale.language == defaultLocale.language  } ?: localesForCountry[0]
        }
    }
}