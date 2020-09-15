package org.totschnig.myexpenses.feature

import androidx.fragment.app.FragmentManager
import java.io.File

interface OcrFeatureProvider {
    fun start(scanFile: File, fragmentManager: FragmentManager)
}
