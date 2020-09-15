package org.totschnig.ocr

import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.feature.OcrFeatureProvider
import java.io.File

object OcrFeatureProviderImpl: OcrFeatureProvider {
    override fun start(scanFile: File, fragmentManager: FragmentManager) {
        ScanPreviewFragment.with(scanFile).show(fragmentManager, "SCAN_PREVIEW")
    }
}