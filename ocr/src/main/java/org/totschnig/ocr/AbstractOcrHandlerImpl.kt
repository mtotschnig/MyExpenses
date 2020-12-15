package org.totschnig.ocr

import android.content.Context
import android.content.Intent
import android.graphics.Rect
import android.text.TextUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.LocalTime
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.format.FormatStyle
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.feature.OcrFeature
import org.totschnig.myexpenses.feature.OcrResult
import org.totschnig.myexpenses.feature.Payee
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.util.locale.UserLocaleProvider
import timber.log.Timber
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

abstract class AbstractOcrHandlerImpl(val prefHandler: PrefHandler, userLocaleProvider: UserLocaleProvider, private val context: Context) : OcrHandler {
    private val numberFormatList: List<NumberFormat>
    private val dateFormatterList: List<DateTimeFormatter>
    private val timeFormatterList: List<DateTimeFormatter>
    private val totalIndicators: List<String>

    init {
        numberFormatList = mutableListOf<NumberFormat>().apply {
            val userFormat = NumberFormat.getInstance(userLocaleProvider.systemLocale)
            add(userFormat)
            val rootFormat = NumberFormat.getInstance(Locale.ROOT)
            if (rootFormat != userFormat) {
                add(rootFormat)
            }
        }
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

    private fun Rect?.bOr0() = this?.bottom ?: 0
    private fun Rect?.tOr0() = this?.top ?: 0
    private fun Line.bOr0() = boundingBox.bOr0()
    private fun Line.tOr0() = boundingBox.tOr0()

    suspend fun queryPayees() = withContext(Dispatchers.Default) {
        mutableListOf<Payee>().also {
            context.contentResolver.query(TransactionProvider.PAYEES_URI,
                    arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_PAYEE_NAME),
                    null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    do {
                        it.add(Payee(cursor.getLong(0), cursor.getString(1)))
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
            //find amount in the total line itself and in the nearest line
            listOf(extractAmount(totalBlock), lines.minus(totalBlock).minByOrNull { line ->
                (line.bOr0() - totalBlock.bOr0()).absoluteValue.coerceAtMost((line.tOr0() - totalBlock.tOr0()).absoluteValue).also {
                    log("%s: distance %d", line.text, it)
                }
            }?.let {
                extractAmount(it)
            })
        }.flatten().filterNotNull()
        // We might receive data or time values split into adjacent elements, which prevents us from finding them, if we work on elements alone
        // if we do not find any data by working on individual elements, we iterate again over pairs, then triples of elements
        val timeCandidates: List<Pair<LocalTime, Rect?>> = lines.mapNotNull { line -> extractTime(line, 1) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractTime(line, 2) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractTime(line, 3) }
        val dateCandidates: List<Pair<LocalDate, LocalTime?>> = lines.mapNotNull { line -> extractDate(line, timeCandidates, 1) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractDate(line, timeCandidates, 2) }.takeIf { it.isNotEmpty() }
                ?: lines.mapNotNull { line -> extractDate(line, timeCandidates, 3) }

        val payeeCandidates = lines
                .map { line -> Utils.normalize(line.text) }
                .mapNotNull { text ->
            payeeList.find { payee ->
                val normalized = Utils.normalize(payee.name)
                startsWith2Ways(normalized, text) || startsWith2Ways(normalized.replace(" ", ""), text.replace(" ", ""))
            }
        }

        return OcrResult(amountCandidates.distinct(), dateCandidates.distinct(), payeeCandidates.distinct())
    }

    private fun startsWith2Ways(one: String, two: String) =
            one.startsWith(two, ignoreCase = true) || two.startsWith(one, ignoreCase = true)

    private val List<Element>.text: String
        get() = joinToString(separator = "") { it.text }
    private val List<Element>.boundingBox: Rect
        get() = Rect().apply { map { it.boundingBox }.forEach { it?.let { union(it) } } }

    private fun extractTime(line: Line, windowSize: Int): Pair<LocalTime, Rect?>? {
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

    private fun extractDate(line: Line, timeCandidates: List<Pair<LocalTime, Rect?>>, windowSize: Int): Pair<LocalDate, LocalTime?>? {
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

    private fun extractAmount(line: Line): String? = line.elements.filter { element ->
        numberFormatList.forEach {
            try {
                it.parse(element.text)
                return@filter true
            } catch (e: Exception) {}
        }
        return@filter false
    }.map { it.text }.takeIf { it.isNotEmpty() }?.joinToString(separator = "")

    fun log(message: String, vararg args: Any?) {
        Timber.tag(OcrFeature.TAG).i(message, *args)
    }
}