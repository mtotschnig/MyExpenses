package org.totschnig.ocr

import android.content.Context
import java.io.File

interface Engine {
    suspend fun run(file: File, context: Context): Text
}