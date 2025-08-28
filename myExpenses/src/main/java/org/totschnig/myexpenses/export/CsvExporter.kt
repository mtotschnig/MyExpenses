package org.totschnig.myexpenses.export

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.TransactionDTO
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.filter.Criterion
import java.time.format.DateTimeFormatter

/**
 * @param account          Account to print
 * @param filter           only transactions matched by filter will be considered
 * @param notYetExportedP  if true only transactions not marked as exported will be handled
 * @param dateFormat       format that can be parsed by SimpleDateFormat class
 * @param decimalSeparator , or .
 * @param encoding         the string describing the desired character encoding.
 * @param delimiter   , or ; or \t
 * @param withAccountColumn put account in column
 */
class CsvExporter(
    account: Account,
    currencyContext: CurrencyContext,
    filter: Criterion?,
    notYetExportedP: Boolean,
    dateFormat: String,
    decimalSeparator: Char,
    encoding: String,
    private val withHeader: Boolean,
    private val delimiter: Char,
    private val withAccountColumn: Boolean,
    private val splitCategoryLevels: Boolean = false,
    private val splitAmount: Boolean = true,
    timeFormat: String? = null,
    private val withOriginalAmount: Boolean = false,
    private val withEquivalentAmountHeader: Boolean = false,
    override val withEquivalentAmount: Boolean = false,
    override val categoryPathSeparator: String = " > ",
    private val withCurrencyColumn: Boolean = false
) :
    AbstractExporter(
        account, currencyContext, filter, notYetExportedP, dateFormat,
        decimalSeparator, encoding
    ) {
    private val timeFormatter: DateTimeFormatter? = timeFormat?.let { DateTimeFormatter.ofPattern(it)  }

    private var numberOfCategoryColumns = 2

    override fun export(
        context: Context,
        outputStream: Lazy<Result<DocumentFile>>,
        append: Boolean
    ): Result<DocumentFile> {
        numberOfCategoryColumns = context.contentResolver.query(
            BaseTransactionProvider.CATEGORY_TREE_URI,
            arrayOf("max(${DatabaseConstants.KEY_LEVEL})"),
            null, null, null
        )?.use {
            it.moveToFirst()
            it.getInt(0)
        } ?: numberOfCategoryColumns
        return super.export(context, outputStream, append)
    }

    override val format = ExportFormat.CSV

    override fun sanitizeCategoryLabel(label: String) = label

    override fun header(context: Context) = if (withHeader) {
        val columns = buildList {
            add(context.getString(R.string.split_transaction))
            add(context.getString(R.string.date))
            if (timeFormatter != null) {
                add(context.getString(R.string.time))
            }
            add(context.getString(R.string.payer_or_payee))
            if (splitAmount) {
                add(context.getString(R.string.income))
                add(context.getString(R.string.expense))
            } else {
                add(context.getString(R.string.amount))
            }
            if (withCurrencyColumn) {
                add(context.getString(R.string.currency))
            }
            if (splitCategoryLevels) {
                repeat(numberOfCategoryColumns) {
                    add(context.getString(R.string.category) + " " + (it +1))
                }
            } else {
                add(context.getString(R.string.category))
            }
            add(context.getString(R.string.notes))
            add(context.getString(R.string.method))
            add(context.getString(R.string.status))
            add(context.getString(R.string.reference_number))
            add(context.getString(R.string.attachments))
            add(context.getString(R.string.tags))
            if (withOriginalAmount) {
                add(context.getString(R.string.menu_original_amount))
                add(context.getString(R.string.menu_original_amount) + " (" + context.getString(R.string.currency) + ")")
            }
            if (withEquivalentAmountHeader) {
                add(context.getString(R.string.menu_equivalent_amount))
            }
        }
        StringBuilder().apply {
            if (withAccountColumn) {
                appendQ(context.getString(R.string.account)).append(delimiter)
            }
            val iterator = columns.iterator()
            while (iterator.hasNext()) {
                val column = iterator.next()
                appendQ(column)
                if (iterator.hasNext()) {
                    append(delimiter)
                }
            }
            append("\n")
        }.toString()
    } else null

    private fun TransactionDTO.handleLabel(stringBuilder: StringBuilder) {
        with(stringBuilder) {
            if (splitCategoryLevels) {
                val path = catId?.let { categoryPaths[catId] }
                repeat(numberOfCategoryColumns) {
                    if (transferAccount != null) {
                        appendQ(if (it == 0) "[$transferAccount]" else "")
                    } else {
                        appendQ(path?.getOrNull(it))
                    }
                    append(delimiter)
                }
            } else {
                appendQ(fullLabel(categoryPaths))
                append(delimiter)
            }
        }
    }

    private fun TransactionDTO.handleAmount(stringBuilder: StringBuilder) {
        with(stringBuilder) {
            if (splitAmount) {
                val amountAbsCSV = nfFormat.format(amount.abs())
                appendQ((if (amount.signum() == 1) amountAbsCSV else "0"))
                append(delimiter)
                appendQ((if (amount.signum() == -1) amountAbsCSV else "0"))
            } else {
                appendQ(nfFormat.format(amount))
            }
            append(delimiter)
        }
    }

    private fun TransactionDTO.handleDateTime(stringBuilder: StringBuilder) {
        with(stringBuilder) {
            appendQ(dateFormatter.format(date))
            append(delimiter)
            if (timeFormatter != null) {
                appendQ(timeFormatter.format(date))
                append(delimiter)
            }
        }
    }

    private fun StringBuilder.handleList(list: List<String>?) {
        appendQ(list?.joinToString(", ") { if (it.contains(',')) "'$it'" else it })
    }

    override val useCategoryOfFirstPartForParent = false

    private fun StringBuilder.appendQ(s: String?) : StringBuilder {
        append('"')
        s?.let { append(s.replace("\"", "\"\"")) }
        append('"')
        return this
    }

    override fun TransactionDTO.marshall(categoryPaths: Map<Long, List<String>>) =
        StringBuilder().apply {
            if (withAccountColumn) {
                appendQ(account.label).append(delimiter)
            }
            val splitIndicator = if (splits != null) SplitTransaction.CSV_INDICATOR else ""
            appendQ(splitIndicator)
            append(delimiter)
            handleDateTime(this)
            appendQ(payee)
            append(delimiter)
            handleAmount(this)
            if (withCurrencyColumn) {
                appendQ(currency)
                append(delimiter)
            }
            handleLabel(this)
            appendQ(comment)
            append(delimiter)
            appendQ(methodLabel)
            append(delimiter)
            appendQ(status?.symbol?.toString())
            append(delimiter)
            appendQ(referenceNumber)
            append(delimiter)
            handleList(attachmentFileNames)
            append(delimiter)
            handleList(tagList)
            if (withOriginalAmount) {
                append(delimiter)
                if (originalCurrency != null) {
                    appendQ(
                        nfFormats.getValue(currencyContext[originalCurrency]).format(originalAmount)
                    )
                }
                append(delimiter)
                if (originalCurrency != null) {
                    appendQ(originalCurrency)
                }
            }
            if (withEquivalentAmountHeader) {
                append(delimiter)
                equivalentAmount?.let {
                    appendQ(nfFormats.getValue(currencyContext.homeCurrencyUnit).format(it))
                }
            }

            splits?.forEach {
                append("\n")
                if (withAccountColumn) {
                    appendQ("").append(delimiter)
                }
                appendQ(SplitTransaction.CSV_PART_INDICATOR)
                append(delimiter)
                it.handleDateTime(this@apply)
                appendQ(payee)
                append(delimiter)
                it.handleAmount(this@apply)
                it.handleLabel(this@apply)
                appendQ(it.comment)
                append(delimiter)
                appendQ("")
                append(delimiter)
                appendQ("")
                append(delimiter)
                appendQ("")
                append(delimiter)
                appendQ("")
                append(delimiter)
                handleList(it.tagList)
            }
        }.toString()
}