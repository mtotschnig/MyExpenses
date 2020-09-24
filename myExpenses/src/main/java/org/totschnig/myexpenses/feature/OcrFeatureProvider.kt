package org.totschnig.myexpenses.feature

import android.os.Parcelable
import androidx.fragment.app.FragmentActivity
import kotlinx.android.parcel.Parcelize
import java.io.File
import java.time.LocalDate
import java.time.LocalTime

interface OcrFeatureProvider {
    fun start(scanFile: File, fragmentActivity: FragmentActivity)
}

@Parcelize
data class OcrResult(val amountCandidates: List<String>, val dateCandidates: List<Pair<LocalDate, LocalTime?>>, val payee: Payee?): Parcelable

@Parcelize
data class Payee(val id: Long, val name: String): Parcelable

interface OcrHost {
    fun processOcrResult(result: Result<OcrResult>)
}
