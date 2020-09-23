package org.totschnig.myexpenses.feature

import androidx.fragment.app.FragmentActivity
import java.io.File

interface OcrFeatureProvider {
    fun start(scanFile: File, fragmentActivity: FragmentActivity)
}

data class OcrResult(val amount: String)
