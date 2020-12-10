package org.totschnig.ocr

import android.content.Context
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import java.io.File
import javax.inject.Inject

class OcrFeatureImpl @Inject constructor(prefHandler: PrefHandler, userLocaleProvider: UserLocaleProvider, context: Context): AbstractOcrFeatureImpl(prefHandler, userLocaleProvider, context) {
    var engine: Engine

    init {
        //TODO instantiate based on preference
        engine = Class.forName("org.totschnig.tesseract.Engine").kotlin.objectInstance as Engine
    }

    override fun initialize(context: Context) {
        engine.initialize(context)
    }
    override suspend fun runTextRecognition(file: File, context: Context): OcrResult =
            processTextRecognitionResult(engine.run(file, context), queryPayees())
}