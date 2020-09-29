package org.totschnig.ocr

import androidx.annotation.Keep
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.feature.OcrFeatureProvider
import java.io.File

abstract class AbstractOcrFeatureProviderImpl: OcrFeatureProvider {
    override fun start(scanFile: File, fragmentActivity: FragmentActivity) {
        ScanPreviewFragment.with(scanFile).show(fragmentActivity.supportFragmentManager, "SCAN_PREVIEW")
    }
}