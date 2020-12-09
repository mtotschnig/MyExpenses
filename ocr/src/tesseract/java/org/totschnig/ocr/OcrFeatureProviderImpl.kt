package org.totschnig.ocr

import androidx.annotation.Keep

@Keep
object OcrFeatureProviderImpl: AbstractOcrFeatureProviderImpl() {
    init {
        System.loadLibrary("jpeg")
        System.loadLibrary("png")
        System.loadLibrary("leptonica")
        System.loadLibrary("tesseract")
    }
}