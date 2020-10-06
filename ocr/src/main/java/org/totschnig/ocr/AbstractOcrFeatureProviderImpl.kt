package org.totschnig.ocr

import android.content.Intent
import androidx.fragment.app.FragmentActivity
import org.totschnig.myexpenses.feature.OcrFeatureProvider
import java.io.File

const val FRAGMENT_TAG = "SCAN_PREVIEW"

abstract class AbstractOcrFeatureProviderImpl: OcrFeatureProvider {
    override fun start(scanFile: File, fragmentActivity: FragmentActivity) {
        ScanPreviewFragment.with(scanFile).show(fragmentActivity.supportFragmentManager, FRAGMENT_TAG)
    }

    override fun handleData(intent: Intent, fragmentActivity: FragmentActivity) {
        (fragmentActivity.supportFragmentManager.findFragmentByTag(FRAGMENT_TAG) as? ScanPreviewFragment)?.handleData(intent)
    }
}