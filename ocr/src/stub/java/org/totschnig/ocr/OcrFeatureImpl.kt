package org.totschnig.ocr

import android.content.Context
import java.io.File
import javax.inject.Inject

class OcrFeatureImpl @Inject constructor() : OcrFeature {
    override suspend fun runTextRecognition(file: File, context: Context): List<String> {
        TODO("Not yet implemented")
    }
}