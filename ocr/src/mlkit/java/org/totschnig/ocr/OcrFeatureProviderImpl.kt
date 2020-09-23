package org.totschnig.ocr

import androidx.annotation.Keep
import androidx.fragment.app.FragmentActivity
import com.google.mlkit.common.MlKit
import java.io.File

@Keep
object OcrFeatureProviderImpl: AbstractOcrFeatureProviderImpl() {
    var initialized: Boolean = false

    override fun start(scanFile: File, fragmentActivity: FragmentActivity) {
        if (!initialized) {
            MlKit.initialize(fragmentActivity)
            initialized = true
        }
        super.start(scanFile, fragmentActivity)
    }
}