package org.totschnig.ocr

import android.content.Context
import org.totschnig.myexpenses.preference.PrefHandler
import java.io.File

interface Engine {
    suspend fun run(file: File, context: Context, prefHandler: PrefHandler): Text
}

interface TesseractEngine : Engine  {
    fun downloadTessData(context: Context, language: String)
    fun tessDataExists(context: Context, language: String): Boolean
}