package org.totschnig.myexpenses.feature

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import androidx.fragment.app.FragmentManager
import androidx.preference.ListPreference
import kotlinx.parcelize.Parcelize
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.totschnig.myexpenses.activity.BaseActivity
import java.io.File

interface OcrFeature {
    companion object {
        const val TAG = "OcrFeature"
        const val ACTION = "org.totschnig.ocr.action.RECOGNIZE"
        const val MIME_TYPE = "image/jpeg"
        fun intent() = Intent(ACTION).setType(MIME_TYPE)
    }
    fun start(scanFile: File, fragmentManager: FragmentManager) {}
    fun handleData(intent: Intent?, fragmentManager: FragmentManager) {}
    fun downloadTessData(context: Context): String? = null
    fun isAvailable(context: Context): Boolean = false
    fun offerInstall(baseActivity: BaseActivity) {}
    fun configureTesseractLanguagePref(listPreference: ListPreference) {
        listPreference.isVisible = false
    }
    fun shouldShowEngineSelection() = false
}

@Parcelize
data class OcrResult(val amountCandidates: List<String>, val dateCandidates: List<Pair<LocalDate, LocalTime?>>, val payeeCandidates: List<Payee>): Parcelable {
    fun isEmpty() = amountCandidates.isEmpty() && dateCandidates.isEmpty() && payeeCandidates.isEmpty()
    fun needsDisambiguation() = amountCandidates.size > 1 || dateCandidates.size > 1 || payeeCandidates.size > 1
    fun selectCandidates(amountIndex: Int = 0, dateIndex: Int = 0, payeeIndex: Int = 0) =
            OcrResultFlat(amountCandidates.getOrNull(amountIndex), dateCandidates.getOrNull(dateIndex), payeeCandidates.getOrNull(payeeIndex))
}

@Parcelize
data class OcrResultFlat(val amount: String?, val date: Pair<LocalDate, LocalTime?>?, val payee: Payee?): Parcelable

@Parcelize
data class Payee(val id: Long, val name: String): Parcelable

interface OcrHost {
    fun processOcrResult(result: Result<OcrResult>)
}
