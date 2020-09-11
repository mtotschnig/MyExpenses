package org.totschnig.myexpenses.feature

import android.content.ContentResolver
import android.net.Uri
import org.totschnig.myexpenses.preference.PrefHandler

interface OcrFeature {
    suspend fun runTextRecognition(imageUri: Uri): List<String>

    interface Provider {
        fun get(contentResolver: ContentResolver, prefHandler: PrefHandler): OcrFeature
    }
}
