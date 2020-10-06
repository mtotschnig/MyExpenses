package org.totschnig.ocr

import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.feature.OcrResult
import java.io.File

interface OcrFeature {
    suspend fun runTextRecognition(file: File, context: Context): OcrResult
    fun handleData(intent: Intent): OcrResult
}