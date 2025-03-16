package org.totschnig.ocr

import android.graphics.Rect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import timber.log.Timber
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.absoluteValue

abstract class AbstractOcrHandlerImpl(
    @Suppress("MemberVisibilityCanBePrivate") val prefHandler: PrefHandler,
    private val application: MyApplication
) : OcrHandler {
    private val numberFormatList: List<NumberFormat>
    private val dateFormatterList: List<DateTimeFormatter>
    private val timeFormatterList: List<DateTimeFormatter>
    private val totalIndicators: List<String>

    val locale: Locale
        get() = application.userPreferredLocale


    init {
        numberFormatList = mutableListOf<NumberFormat>().apply {
            add(NumberFormat.getCurrencyInstance(locale))
            val userFormat = NumberFormat.getInstance(locale)
            add(userFormat)
            val rootFormat = NumberFormat.getInstance(Locale.ROOT)
            if (rootFormat != userFormat) {
                add(rootFormat)
            }
        }
        val withSystemLocale: (DateTimeFormatter) -> DateTimeFormatter = { it.withLocale(locale) }
        dateFormatterList =
            prefHandler.getString(PrefKey.OCR_DATE_FORMATS, null)?.lines()?.mapNotNull {
                try {
                    DateTimeFormatter.ofPattern(it, locale)
                } catch (_: Exception) {
                    null
                }
            } ?: listOf(
                DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT),
                DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
            )
                .map(withSystemLocale)
        timeFormatterList =
            prefHandler.getString(PrefKey.OCR_TIME_FORMATS, null)?.lines()?.mapNotNull {
                try {
                    DateTimeFormatter.ofPattern(it, locale)
                } catch (_: Exception) {
                    null
                }
            } ?: listOf(
                DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT),
                DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM)
            )
                .map(withSystemLocale)
        totalIndicators = prefHandler.getString(PrefKey.OCR_TOTAL_INDICATORS, null)
            ?.parseTotalIndicators()
            ?: application.wrappedContext.getString(R.string.pref_ocr_total_indicators_default)
                .parseTotalIndicators()
                    ?: listOf("Total")
        log("TotalIndicators: %s", totalIndicators.joinToString("\n"))
    }

    private fun String.parseTotalIndicators() = lines()
        .map { it.filter { c -> c.isLetter() } }
        .filter { it.isNotEmpty() }
        .distinct()
        .takeIf { it.isNotEmpty() }

    private fun Rect?.bOr0() = this?.bottom ?: 0
    private fun Rect?.tOr0() = this?.top ?: 0
    private fun Line.bOr0() = boundingBox.bOr0()
    private fun Line.tOr0() = boundingBox.tOr0()

    suspend fun queryPayees() = withContext(Dispatchers.Default) {
        buildList {
            application.contentResolver.query(
                TransactionProvider.PAYEES_URI,
                arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PAYEE_NAME),
                null, null, null
            )?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        val name = cursor.getString(1)
                        if (name.isNotBlank()) {
                            add(Payee(cursor.getLong(0), name))
                        }
                    } while (cursor.moveToNext())
                }
            }
        }
    }

    fun processTextRecognitionResult(texts: Text, payeeList: List<Payee>): OcrResult {
        if (dateFormatterList.isEmpty()) {
            throw IllegalStateException("Empty date format list")
        }
        if (timeFormatterList.isEmpty()) {
            throw IllegalStateException("Empty time format list")
        }
        val blocks = texts.textBlocks
        val lines = mutableListOf<Line>()
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
                if (line.text.filter { c -> c.isLetter() }
                        .startsWith(totalIndicator, ignoreCase = true)) return@filter true
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
            log("Now looking at total block: ${totalBlock.text}")
            //find amount in the total line itself and in the nearest line
            listOf(extractAmount(totalBlock), lines.minus(totalBlock).sortedBy { line ->
                (line.bOr0() - totalBlock.bOr0()).absoluteValue.coerceAtMost((line.tOr0() - totalBlock.tOr0()).absoluteValue)
                    .also {
                        log("%s: distance %d", line.text, it)
                    }
            }.firstNotNullOfOrNull {
                extractAmount(it)
            })
        }.flatten().filterNotNull()
        // We might receive data or time values split into adjacent elements, which prevents us from finding them, if we work on elements alone
        // if we do not find any data by working on individual elements, we iterate again over pairs, then triples of elements
        val timeCandidates: List<Pair<LocalTime, Rect?>> =
            lines.mapNotNull { line -> extractTime(line, 1) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractTime(line, 2) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractTime(line, 3) }
        val dateCandidates: List<Pair<LocalDate, LocalTime?>> =
            lines.mapNotNull { line -> extractDate(line, timeCandidates, 1) }
                .takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractDate(line, timeCandidates, 2) }
                    .takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractDate(line, timeCandidates, 3) }

        val payeeCandidates = lines
            .map { line -> Utils.normalize(line.text) }
            .mapNotNull { text ->
                payeeList.find { payee ->
                    val normalized = Utils.normalize(payee.name)
                    startsWith2Ways(normalized, text) || startsWith2Ways(
                        normalized.replace(
                            " ",
                            ""
                        ), text.replace(" ", "")
                    )
                }
            }

        return OcrResult(
            amountCandidates.distinct(),
            dateCandidates.distinct(),
            payeeCandidates.distinct()
        )
    }

    private fun startsWith2Ways(one: String, two: String) =
        one.startsWith(two, ignoreCase = true) || two.startsWith(one, ignoreCase = true)

    private fun List<Element>.join(separator: CharSequence = "") =
        joinToString(separator) { it.text }

    private val List<Element>.boundingBox: Rect
        get() = Rect().apply { map { it.boundingBox }.forEach { it?.let { union(it) } } }

    private fun extractTime(line: Line, windowSize: Int): Pair<LocalTime, Rect?>? {
        timeFormatterList.forEach { formatter ->
            line.elements.windowed(windowSize).forEach { list ->
                (try {
                    LocalTime.parse(list.join(), formatter)
                } catch (_: Exception) {
                    null
                }
                    ?: try {
                        LocalTime.parse(list.join(" "), formatter)
                    } catch (_: Exception) {
                        null
                    })?.let {
                        return Pair(it, list.boundingBox)
                }
            }
        }
        return null
    }

    private fun extractDate(
        line: Line,
        timeCandidates: List<Pair<LocalTime, Rect?>>,
        windowSize: Int
    ): Pair<LocalDate, LocalTime?>? {
        dateFormatterList.forEach { formatter ->
            line.elements.windowed(windowSize).forEach { list ->
                try {
                    val date = LocalDate.parse(list.join(), formatter)
                    return Pair(
                        date,
                        timeCandidates.minByOrNull {
                            (it.second.bOr0() - list.boundingBox.bottom).absoluteValue.coerceAtMost(
                                (it.second.tOr0() - list.boundingBox.top).absoluteValue
                            )
                        }?.first
                    )
                } catch (_: Exception) {
                }
            }
        }
        return null
    }

    private fun extractAmount(line: Line): String? = line.elements.filter { element ->
        numberFormatList.forEach {
            try {
                it.parse(element.text)
                return@filter true
            } catch (_: Exception) {
            }
        }
        return@filter false
    }.map { it.text }.takeIf { it.isNotEmpty() }?.joinToString(separator = "")

    private fun log(message: String, vararg args: Any?) {
        Timber.tag(OcrFeature.TAG).i(message, *args)
    }
}