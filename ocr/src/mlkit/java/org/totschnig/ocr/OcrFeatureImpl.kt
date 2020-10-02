package org.totschnig.ocr

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import android.text.TextUtils
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.feature.OcrFeatureProvider
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import timber.log.Timber
import java.io.File
import java.text.NumberFormat
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class OcrFeatureImpl @Inject constructor(prefHandler: PrefHandler, userLocaleProvider: UserLocaleProvider, context: Context) : OcrFeature {

    private val numberFormat = NumberFormat.getInstance()
    private val dateFormatterList: List<DateTimeFormatter>
    private val timeFormatterList: List<DateTimeFormatter>
    private val totalIndicators: List<String>

    init {
        val withSystemLocale: (DateTimeFormatter) -> DateTimeFormatter = { it.withLocale(userLocaleProvider.systemLocale) }
        dateFormatterList = prefHandler.getString(PrefKey.OCR_DATE_FORMATS, null)?.lines()?.mapNotNull {
            try {
                DateTimeFormatter.ofPattern(it, userLocaleProvider.systemLocale)
            } catch (e: Exception) {
                null
            }
        } ?: listOf(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT),
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                .map(withSystemLocale)
        timeFormatterList = prefHandler.getString(PrefKey.OCR_TIME_FORMATS, null)?.lines()?.mapNotNull {
            try {
                DateTimeFormatter.ofPattern(it, userLocaleProvider.systemLocale)
            } catch (e: Exception) {
                null
            }
        } ?: listOf(
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
                DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))
                .map(withSystemLocale)
        totalIndicators = (prefHandler.getString(PrefKey.OCR_TOTAL_INDICATORS, null).takeIf { !TextUtils.isEmpty(it) }
                ?: context.getString(R.string.pref_ocr_total_indicators_default)).lines()
    }

    override suspend fun runTextRecognition(file: File, context: Context): OcrResult {
        if (dateFormatterList.isEmpty()) {
            throw IllegalStateException("Empty date format list")
        }
        if (timeFormatterList.isEmpty()) {
            throw IllegalStateException("Empty time format list")
        }
        @Suppress("BlockingMethodInNonBlockingContext") val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        val payeeList = mutableListOf<Payee>()
        context.contentResolver.query(TransactionProvider.PAYEES_URI,
                arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PAYEE_NAME),
                null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                do {
                    payeeList.add(Payee(cursor.getLong(0), cursor.getString(1)))
                } while (cursor.moveToNext())
            }
        }
        return suspendCoroutine { cont ->
            TextRecognition.getClient().process(image)
                    .addOnSuccessListener { texts ->
                        cont.resume(processTextRecognitionResult(texts, payeeList))
                    }
                    .addOnFailureListener { e ->
                        cont.resumeWithException(e as Throwable)
                    }
        }
    }

    private fun Rect?.bOr0() = this?.bottom ?: 0
    private fun Rect?.tOr0() = this?.top ?: 0
    private fun Text.Line.bOr0() = boundingBox.bOr0()
    private fun Text.Line.tOr0() = boundingBox.tOr0()

    private fun processTextRecognitionResult(texts: Text, payeeList: MutableList<Payee>): OcrResult {
        val blocks = texts.textBlocks
        val lines = mutableListOf<Text.Line>()
        for (i in blocks.indices) {
            lines.addAll(blocks[i].lines)
        }
        for (line in lines) {
            log("OCR: Line: %s %s", line.text, line.boundingBox)
            for (element in line.elements) {
                log("OCR: Element: %s %s", element.text, element.boundingBox)
            }
        }
        val amountCandidates = //find amount in the total line itself or in the nearest line
                lines.filter { line ->
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
                }.mapNotNull { totalBlock ->
                    //find amount in the total line itself or in the nearest line
                    extractAmount(totalBlock)
                            ?: lines.minus(totalBlock).minByOrNull { (it.bOr0() - totalBlock.bOr0()).absoluteValue.coerceAtMost((it.tOr0() - totalBlock.tOr0()).absoluteValue) }?.let {
                                extractAmount(it)
                            }
                }
        // We might receive data or time values split into adjacent elements, which prevents us from finding them, if we work on elements alone
        // if we do not find any data by working on individual elements, we iterate again over pairs, then triples of elements
        val timeCandidates: List<Pair<LocalTime, Rect?>> = lines.mapNotNull { line -> extractTime(line, 1) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractTime(line, 2) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractTime(line, 3) }
        val dateCandidates: List<Pair<LocalDate, LocalTime?>> = lines.mapNotNull { line -> extractDate(line, timeCandidates, 1) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractDate(line, timeCandidates, 2) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractDate(line, timeCandidates, 3) }

        val payeeCandidates = lines.mapNotNull { line ->
            payeeList.find { payee -> payee.name.startsWith(line.text, ignoreCase = true) || line.text.startsWith(payee.name, ignoreCase = true) }
        }

        return OcrResult(amountCandidates.distinct(), dateCandidates.distinct(), payeeCandidates.distinct())
    }

    private val List<Text.Element>.text: String
        get() = joinToString(separator = "") { it.text }
    private val List<Text.Element>.boundingBox: Rect
        get() = Rect().apply { map { it.boundingBox }.forEach { it?.let { union(it) } } }

    private fun extractTime(line: Text.Line, windowSize: Int): Pair<LocalTime, Rect?>? {
        timeFormatterList.forEach { formatter ->
            line.elements.windowed(windowSize).forEach { list ->
                try {
                    return Pair(LocalTime.parse(list.text, formatter), list.boundingBox)
                } catch (e: Exception) {
                }
            }
        }
        return null
    }

    private fun extractDate(line: Text.Line, timeCandidates: List<Pair<LocalTime, Rect?>>, windowSize: Int): Pair<LocalDate, LocalTime?>? {
        dateFormatterList.forEach { formatter ->
            line.elements.windowed(windowSize).forEach { list ->
                try {
                    val date = LocalDate.parse(list.text, formatter)
                    return Pair(date, timeCandidates.minByOrNull { (it.second.bOr0() - list.boundingBox.bottom).absoluteValue.coerceAtMost((it.second.tOr0() - list.boundingBox.top).absoluteValue) }?.first)
                } catch (e: Exception) {
                }
            }
        }
        return null
    }

    private fun extractAmount(line: Text.Line): String? = line.elements.filter { element ->
        try {
            numberFormat.parse(element.text)
            true
        } catch (e: Exception) {
            false
        }
    }.map { it.text }.takeIf { it.isNotEmpty() }?.joinToString(separator = "")

    private fun log(message: String, vararg args: Any?) {
        Timber.tag(OcrFeatureProvider.TAG).i(message, *args)
    }
}