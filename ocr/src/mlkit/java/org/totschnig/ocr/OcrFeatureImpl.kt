package org.totschnig.ocr

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import timber.log.Timber
import java.io.File
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class OcrFeatureImpl @Inject constructor(private val prefHandler: PrefHandler) : OcrFeature {

    private val numberFormat = NumberFormat.getInstance()

    private val dateFormatters = listOf(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT), DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
    private val timeFormatters = listOf(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT), DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))

    override suspend fun runTextRecognition(file: File, context: Context): OcrResult {
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val payeeList = mutableListOf<Payee>()
        context.contentResolver.query(TransactionProvider.PAYEES_URI,
                arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PAYEE_NAME),
                null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    payeeList.add(Payee(cursor.getLong(0), cursor.getString(1)))
                } while (cursor.moveToNext());
            }
        }
        return suspendCoroutine { cont ->
            TextRecognition.getClient().process(image)
                    .addOnSuccessListener { texts ->
                        cont.resume(processTextRecognitionResult(texts, prefHandler.getString(PrefKey.OCR_TOTAL_INDICATORS, "Total")!!.lines(), payeeList))
                    }
                    .addOnFailureListener { e ->
                        cont.resumeWithException(e as Throwable)
                    }
        }
    }
    fun Rect?.bOr0() = this?.bottom ?: 0
    fun Rect?.tOr0() = this?.top ?: 0
    fun Text.Line.bOr0() = boundingBox.bOr0()
    fun Text.Line.tOr0() = boundingBox.tOr0()
    fun Text.Element.bOr0() = boundingBox.bOr0()
    fun Text.Element.tOr0() = boundingBox.tOr0()

    private fun processTextRecognitionResult(texts: Text, totalIndicators: List<String>, payeeList: MutableList<Payee>): OcrResult {
        val blocks = texts.textBlocks
        val lines = mutableListOf<Text.Line>()
        for (i in blocks.indices) {
            lines.addAll(blocks[i].lines)
        }
        for (line in lines) {
            Timber.d("OCR: Line: %s %s", line.text, line.boundingBox)
        }
        val amountCandidates = lines.filter { line ->
            for (totalIndicator in totalIndicators) {
                if (line.text.filter { c -> c.isLetter() }.startsWith(totalIndicator.replace(" ", ""))) return@filter true
                var matchesAllSplits = true
                for (split in totalIndicator.split(' ')) {
                    var found = false
                    for (element in line.elements) {
                        val filter = element.text.filter { c -> c.isLetter() }
                        if (filter.startsWith(split, ignoreCase = true)) {
                            found = true
                            break
                        }
                    }
                    if (!found) {
                        matchesAllSplits = false
                        break
                    }
                }
                if (matchesAllSplits) return@filter true
            }
            false
        }.map { totalBlock ->
            //find amount in the total line itself or in the nearest line
            extractAmount(totalBlock)
                    ?: lines.minus(totalBlock).minByOrNull { (it.bOr0() - totalBlock.bOr0()).absoluteValue.coerceAtMost((it.tOr0() - totalBlock.tOr0()).absoluteValue) }?.let {
                        extractAmount(it)
                    }
        }.filterNotNull()
        val timeCandidates: List<Pair<LocalTime, Rect?>> = lines.map outer@ { line ->
            for (formatter in timeFormatters) {
                line.elements.map {
                    try {
                        return@outer Pair(LocalTime.parse(it.text, formatter), it.boundingBox)
                    } catch (e: Exception) {
                    }
                }
            }
            null
        }.filterNotNull()
        val dateCandidates = lines.map outer@ { line ->
            for (formatter in dateFormatters) {
                line.elements.map { element ->
                    try {
                        val date = LocalDate.parse(element.text, formatter)
                        return@outer Pair(date, timeCandidates.minByOrNull { (it.second.bOr0() - element.bOr0()).absoluteValue.coerceAtMost((it.second.tOr0() - element.tOr0()).absoluteValue) }?.first)
                    } catch (e: Exception) {
                    }
                }
            }
            null
        }.filterNotNull()

        val payee = lines.map { line ->
            payeeList.find { payee -> payee.name.startsWith(line.text, ignoreCase = true) }
        }.first()

        return OcrResult(amountCandidates, dateCandidates, payee)
    }

    fun extractAmount(line: Text.Line): String? = line.elements.filter { element ->
        try {
            numberFormat.parse(element.text)
            true
        } catch (e: Exception) {
            false
        }
    }.map { it.text }.takeIf { !it.isEmpty() }?.joinToString(separator = "")
}