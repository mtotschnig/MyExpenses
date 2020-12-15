package org.totschnig.ocr

import android.content.Context
import org.totschnig.myexpenses.activity.BaseActivity
import org.totschnig.myexpenses.preference.PrefHandler
import java.io.File

interface Engine {
    suspend fun run(file: File, context: Context, prefHandler: PrefHandler): Text
    fun info(context: Context, prefHandler: PrefHandler): CharSequence
}

interface TesseractEngine : Engine  {
    fun downloadTessData(context: Context, prefHandler: PrefHandler): String
    fun tessDataExists(context: Context, prefHandler: PrefHandler): Boolean
    fun offerTessDataDownload(baseActivity: BaseActivity)
    fun getLanguageArray(context: Context): Array<String>
}