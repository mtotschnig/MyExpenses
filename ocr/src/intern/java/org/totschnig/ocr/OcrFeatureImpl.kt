package org.totschnig.ocr

import android.content.Context
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.io.File
import java.util.*
import javax.inject.Inject

class OcrFeatureImpl @Inject constructor(prefHandler: PrefHandler, userLocaleProvider: UserLocaleProvider, context: Context) : AbstractOcrFeatureImpl(prefHandler, userLocaleProvider, context) {
    lateinit var engine: Engine

    override suspend fun runTextRecognition(file: File, context: Context): OcrResult {
        val string = prefHandler.getString(PrefKey.OCR_ENGINE, getDefaultEngine(Utils.localeFromContext(context)))
        engine = Class.forName("org.totschnig.$string.Engine").kotlin.objectInstance as Engine
        return processTextRecognitionResult(engine.run(file, context), queryPayees())
    }

    /**
     * check if language has non-latin script and is supported by Tesseract
     */
    private fun getDefaultEngine(locale: Locale) = if (locale.language in arrayOf(
                    "am", "ar", "as", "be", "bn", "bo", "bg", "zh", "dz", "el", "fa", "gu", "iw", "hi",
                    "iu", "jv", "kn", "ka", "kk", "km", "ky", "ko", "lo", "ml", "mn", "my", "ne", "or",
                    "pa", "ps", "ru", "si", "sd", "sr", "ta", "te", "tg", "th", "ti", "ug", "uk", "ur"))
        "tesseract" else "mlkit"


}