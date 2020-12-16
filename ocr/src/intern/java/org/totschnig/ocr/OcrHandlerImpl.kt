package org.totschnig.ocr

import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.feature.getDefaultEngine
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.io.File
import java.util.*
import javax.inject.Inject

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
                prefHandler.getString(PrefKey.OCR_ENGINE, null) ?: getDefaultEngine(context))

        fun getEngine(engine: String) = try {
            Class.forName("org.totschnig.$engine.Engine").kotlin.objectInstance as Engine
        } catch (e: Exception) {
            null
        }
    }
}