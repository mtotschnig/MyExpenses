package org.totschnig.myexpenses.next.transactions

import java.io.Serializable
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import kotlin.math.absoluteValue

/**
 * What the details of a transaction can change of it
 *
 * @param amount in minor units, negative for an expense
 * @param date seconds since the epoch, like in the database
 * @param valueDate seconds since the epoch, moves with [date] while both are the same day
 */
data class EditValues(
    val amount: Long,
    val date: Long,
    val valueDate: Long,
    val payee: String?,
    val methodId: Long?,
    val comment: String?,
) : Serializable

/**
 * A transaction as loaded for changing it in its details
 *
 * @param accountType id of the type of its account, the payment methods depend on it
 * @param isAmountEditable false if the amount has an original or equivalent amount in another
 * currency, which the details would leave wrong
 * @param isImported comes from the bank import, a change of amount, date or payee no longer
 * matches what the bank reports
 */
data class EditableTransaction(
    val id: Long,
    val values: EditValues,
    val fractionDigits: Int,
    val accountType: Long?,
    val isAmountEditable: Boolean,
    val isImported: Boolean,
)

/** A payment method to choose from */
data class MethodOption(val id: Long, val label: String)

/**
 * What is typed in the edit mode of the details. Kept as typed, so that an amount that cannot be
 * read yet survives, and [Serializable] for the saved state.
 */
data class EditDraft(
    val id: Long,
    val amountText: String,
    val isIncome: Boolean,
    val day: LocalDate,
    val payee: String,
    val methodId: Long?,
    val comment: String,
) : Serializable

/** Which value a quick change in the details changed, for the text of its undo */
enum class ChangedField { COMMENT, DATE, METHOD, ALL }

/** A change that was saved and can be undone */
data class Undo(val id: Long, val previous: EditValues, val field: ChangedField)

/**
 * The amount as typed, without sign: the last separator followed by at most [fractionDigits]
 * digits is the decimal one, the others group thousands, so that "1.234" is a thousand and
 * "12,5" twelve and a half. Null if it is no amount.
 */
fun parseAmountText(text: String, fractionDigits: Int): Long? {
    val trimmed = text.filterNot { it.isWhitespace() || it == ' ' }
    if (trimmed.isEmpty() || trimmed.any { !it.isDigit() && it != ',' && it != '.' }) return null
    val lastSeparator = trimmed.indexOfLast { it == ',' || it == '.' }
    val decimals = if (lastSeparator >= 0) trimmed.length - lastSeparator - 1 else 0
    val isDecimal = lastSeparator >= 0 && decimals in 1..fractionDigits
    val whole = (if (isDecimal) trimmed.substring(0, lastSeparator) else trimmed).filter { it.isDigit() }
    val fraction = if (isDecimal) trimmed.substring(lastSeparator + 1) else ""
    if (whole.isEmpty() && fraction.isEmpty()) return null
    return BigDecimal("${whole.ifEmpty { "0" }}.${fraction.ifEmpty { "0" }}")
        .movePointRight(fractionDigits)
        .longValueExact()
}

/** [amount] in minor units as typed in the edit mode: without sign and grouping */
fun formatAmountText(amount: Long, fractionDigits: Int, decimalSeparator: Char): String =
    BigDecimal.valueOf(amount.absoluteValue, fractionDigits).toPlainString().replace('.', decimalSeparator)

/** The draft of the edit mode for [transaction] */
fun EditableTransaction.toDraft(decimalSeparator: Char, zone: ZoneId) = EditDraft(
    id = id,
    amountText = formatAmountText(values.amount, fractionDigits, decimalSeparator),
    isIncome = values.amount > 0,
    day = values.date.toDay(zone),
    payee = values.payee.orEmpty(),
    methodId = values.methodId,
    comment = values.comment.orEmpty(),
)

/** The values of [draft] applied to [values], null if its amount cannot be read */
fun EditableTransaction.apply(draft: EditDraft, zone: ZoneId): EditValues? {
    val amount = if (isAmountEditable) {
        val typed = parseAmountText(draft.amountText, fractionDigits) ?: return null
        if (draft.isIncome) typed else -typed
    } else values.amount
    return values.withDay(draft.day, zone).copy(
        amount = amount,
        payee = draft.payee.trim().ifEmpty { null },
        methodId = draft.methodId,
        comment = draft.comment.trim().ifEmpty { null },
    )
}

/** [EditValues] moved to [day], keeping the time; the value date moves along if it was the same day */
fun EditValues.withDay(day: LocalDate, zone: ZoneId): EditValues {
    val moved = date.onDay(day, zone)
    return copy(
        date = moved,
        valueDate = if (valueDate.toDay(zone) == date.toDay(zone)) valueDate.onDay(day, zone) else valueDate
    )
}

internal fun Long.toDay(zone: ZoneId): LocalDate = Instant.ofEpochSecond(this).atZone(zone).toLocalDate()

private fun Long.onDay(day: LocalDate, zone: ZoneId): Long {
    val time = Instant.ofEpochSecond(this).atZone(zone).toLocalTime()
    return day.atTime(time).atZone(zone).toEpochSecond()
}

/** Whether amount, day or payee of [other] differ from these values, what the bank reports */
fun EditValues.differsInBankFields(other: EditValues, zone: ZoneId) =
    amount != other.amount || date.toDay(zone) != other.date.toDay(zone) || payee != other.payee
