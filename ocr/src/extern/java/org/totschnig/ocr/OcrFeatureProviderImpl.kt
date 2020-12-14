package org.totschnig.ocr

import androidx.annotation.Keep

@Keep
object OcrFeatureImpl(val prefHandler: PrefHandler): OcrFeature() {
    override fun handleData(intent: Intent, fragmentManager: FragmentManager) {
        (fragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScanPreviewFragment)?.handleData(intent)
    }
}