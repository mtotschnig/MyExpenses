package org.totschnig.myexpenses.next.settings

import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Currency
import java.util.Date
import java.util.Locale

/** Date patterns to pick from, as an example each, see [validateDateFormat][org.totschnig.myexpenses.util.validateDateFormat] */
val datePatterns = listOf("dd.MM.yy", "dd.MM.yyyy", "d.M.yyyy", "yyyy-MM-dd", "dd/MM/yyyy", "MM/dd/yyyy", "dd-MM-yyyy")

/**
 * Number patterns to pick from, in the syntax that does not depend on the locale, see [localizedNumberPattern]
 */
val numberPatterns = listOf("#,##0.00 ¤", "¤ #,##0.00", "¤#,##0.00", "#,##0.00¤", "#0.00 ¤", "#,##0.00 ¤¤")

/** The amount the examples of number formats show */
private const val EXAMPLE_AMOUNT = 1234.56

/** As the settings store a number pattern: with the symbols of the locale */
fun localizedNumberPattern(pattern: String): String = DecimalFormat(pattern).toLocalizedPattern()

/** [date] in [pattern], null if the pattern is not valid */
fun dateExample(pattern: String, locale: Locale, date: Date = Date()): String? =
    runCatching { SimpleDateFormat(pattern, locale).format(date) }.getOrNull()

/**
 * An amount in [localizedPattern], as the CurrencyFormatter applies it, null if the pattern is not
 * valid
 */
fun numberExample(localizedPattern: String, currencyCode: String, currencySymbol: String): String? = runCatching {
    DecimalFormat().apply {
        applyLocalizedPattern(localizedPattern)
        runCatching { currency = Currency.getInstance(currencyCode) }
        decimalFormatSymbols = decimalFormatSymbols.apply { this.currencySymbol = currencySymbol }
    }.format(EXAMPLE_AMOUNT)
}.getOrNull()

/**
 * Pattern to example, the format of the region first, without patterns that look the same
 *
 * @param region labels the example of the format of the region
 */
fun formatOptions(
    regionPattern: String?,
    patterns: List<String>,
    example: (String) -> String?,
    region: (String) -> String,
): List<Pair<String, String>> =
    (listOfNotNull(regionPattern) + patterns)
        .mapNotNull { pattern -> example(pattern)?.let { pattern to it } }
        .distinctBy { it.second }
        .map { (pattern, example) -> pattern to if (pattern == regionPattern) region(example) else example }
