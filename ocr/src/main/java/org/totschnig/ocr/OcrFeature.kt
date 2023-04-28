package org.totschnig.ocr

import android.net.Uri
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.feature.OcrFeature

const val FRAGMENT_TAG = "SCAN_PREVIEW"

@Keep
abstract class OcrFeature: OcrFeature {
    override fun start(scanUri: Uri, fragmentManager: FragmentManager) {
        ScanPreviewFragment.with(scanUri).show(fragmentManager, FRAGMENT_TAG)
    }
}