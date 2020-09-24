package org.totschnig.myexpenses.feature

import androidx.fragment.app.FragmentActivity
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

interface OcrFeatureProvider {
    fun start(scanFile: File, fragmentActivity: FragmentActivity)
}

data class OcrResult(val amountCandidates: List<String>, val dateCandidates: List<Pair<LocalDate, LocalTime?>>)
