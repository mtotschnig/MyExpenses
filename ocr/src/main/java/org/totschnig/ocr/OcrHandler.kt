package org.totschnig.ocr

import android.content.Context
import android.content.Intent
import android.net.Uri
import org.totschnig.myexpenses.feature.OcrResult

interface OcrHandler {
    suspend fun runTextRecognition(uri: Uri, context: Context): OcrResult
    suspend fun handleData(intent: Intent): OcrResult
    fun info(context: Context): CharSequence?
}