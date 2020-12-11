package org.totschnig.ocr

import androidx.annotation.Keep

@Keep
object OcrFeatureProviderImpl: AbstractOcrFeatureProviderImpl() {
    override fun handleData(intent: Intent, fragmentManager: FragmentManager) {
        (fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScanPreviewFragment)?.handleData(intent)
    }
}