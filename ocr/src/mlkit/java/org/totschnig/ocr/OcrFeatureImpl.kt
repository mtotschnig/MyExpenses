package org.totschnig.ocr

import android.content.Context
import android.graphics.Rect
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
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
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlin.math.absoluteValue

class OcrFeatureImpl @Inject constructor(private val prefHandler: PrefHandler, private val userLocaleProvider: UserLocaleProvider) : OcrFeature {

    private val numberFormat = NumberFormat.getInstance()
    private val dateFormatterList: List<DateTimeFormatter>
    private val timeFormatterList: List<DateTimeFormatter>

    init {
        val withSystemLocale: (DateTimeFormatter) -> DateTimeFormatter = { it.withLocale(userLocaleProvider.systemLocale) }
        dateFormatterList = prefHandler.getString(PrefKey.OCR_DATE_FORMATS, null)?.lines()?.map {
            try {
                DateTimeFormatter.ofPattern(it, userLocaleProvider.systemLocale)
            } catch (e: Exception) {
                null
            }
        }?.filterNotNull() ?: listOf(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT),
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                .map(withSystemLocale)
        timeFormatterList = prefHandler.getString(PrefKey.OCR_TIME_FORMATS, null)?.lines()?.map {
            try {
                DateTimeFormatter.ofPattern(it, userLocaleProvider.systemLocale)
            } catch (e: Exception) {
                null
            }
        }?.filterNotNull() ?: listOf(
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
                DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))
                .map(withSystemLocale)
    }

    override suspend fun runTextRecognition(file: File, context: Context): OcrResult {
        if (dateFormatterList.isEmpty()) {
            throw IllegalStateException("Empty date format list")
        }
        if (timeFormatterList.isEmpty()) {
            throw IllegalStateException("Empty time format list")
        }
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

    private fun processTextRecognitionResult(texts: Text, totalIndicators: List<String>, payeeList: MutableList<Payee>): OcrResult {
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
        // We might receive data or time values split into adjacent elements, which prevents us from finding them, if we work on elements alone
        // if we do not find any data by working on individual elements, we iterate again over pairs, then triples of elements
        val timeCandidates: List<Pair<LocalTime, Rect?>> = lines.map { line -> extractTime(line, 1) }.filterNotNull().takeIf { !it.isEmpty() }
                ?: lines.map { line -> extractTime(line, 2) }.filterNotNull().takeIf { !it.isEmpty() }
                ?: lines.map { line -> extractTime(line, 3) }.filterNotNull()
        val dateCandidates: List<Pair<LocalDate, LocalTime?>> = lines.map { line -> extractDate(line, timeCandidates, 1) }.filterNotNull().takeIf { !it.isEmpty() }
                ?: lines.map { line -> extractDate(line, timeCandidates, 2) }.filterNotNull().takeIf { !it.isEmpty() }
                ?: lines.map { line -> extractDate(line, timeCandidates, 3) }.filterNotNull()

        val payeeCandidates = lines.map { line ->
            payeeList.find { payee -> payee.name.startsWith(line.text, ignoreCase = true) || line.text.startsWith(payee.name, ignoreCase = true) }
        }.filterNotNull()

        return OcrResult(amountCandidates, dateCandidates, payeeCandidates)
    }

    val List<Text.Element>.text: String
        get() = map { it.text }.joinToString(separator = "")
    val List<Text.Element>.boundingBox: Rect
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
    }.map { it.text }.takeIf { !it.isEmpty() }?.joinToString(separator = "")

    private fun log(message: String, vararg args: Any?) {
        Timber.tag(OcrFeatureProvider.TAG).i(message, *args)
    }
}