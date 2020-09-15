package org.totschnig.ocr

import android.content.Context
import java.io.File

interface OcrFeature {
    suspend fun runTextRecognition(file: File, context: Context): List<String>
}