package org.totschnig.ocr

import android.content.Context
import org.totschnig.myexpenses.feature.OcrResult
import java.io.File

interface OcrFeature {
    suspend fun runTextRecognition(file: File, context: Context): OcrResult
}