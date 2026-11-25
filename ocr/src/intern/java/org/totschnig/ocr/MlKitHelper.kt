package org.totschnig.ocr

import android.content.Context
import com.google.mlkit.common.MlKit

fun Context.initMlKit() {
    try {
        MlKit.initialize(applicationContext)
    } catch (_: IllegalStateException) {
        // Already initialized
    }
}
