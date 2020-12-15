package org.totschnig.ocr

import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.feature.OcrFeature
import java.io.File

const val FRAGMENT_TAG = "SCAN_PREVIEW"

@Keep
abstract class OcrFeature: OcrFeature {
    override fun start(scanFile: File, fragmentManager: FragmentManager) {
        ScanPreviewFragment.with(scanFile).show(fragmentManager, FRAGMENT_TAG)
    }
}