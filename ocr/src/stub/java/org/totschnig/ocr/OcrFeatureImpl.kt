package org.totschnig.ocr

import android.content.Context
import org.totschnig.myexpenses.feature.OcrResult
import java.io.File
import javax.inject.Inject

class OcrFeatureImpl @Inject constructor() : OcrFeature {
    override suspend fun runTextRecognition(file: File, context: Context): OcrResult? {
        TODO("Not yet implemented")
    }
}