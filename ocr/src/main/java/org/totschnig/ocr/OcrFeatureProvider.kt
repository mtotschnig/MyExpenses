package org.totschnig.ocr

import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import androidx.fragment.app.FragmentManager
import org.totschnig.myexpenses.feature.OcrFeatureProvider
import java.io.File

const val FRAGMENT_TAG = "SCAN_PREVIEW"

@Keep
abstract class OcrFeatureProvider: OcrFeatureProvider {
    override fun start(scanFile: File, fragmentManager: FragmentManager) {
        ScanPreviewFragment.with(scanFile).show(fragmentManager, FRAGMENT_TAG)
    }
}