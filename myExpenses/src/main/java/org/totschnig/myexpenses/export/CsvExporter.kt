package org.totschnig.myexpenses.export

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.model.TransactionDTO
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.StringBuilderWrapper
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
    filter: WhereFilter?,
    notYetExportedP: Boolean,
    dateFormat: String,
    decimalSeparator: Char,
    encoding: String,
    private val withHeader: Boolean,
    private val delimiter: Char,
    private val withAccountColumn: Boolean,
    private val splitCategoryLevels: Boolean = false,
    private val splitAmount: Boolean = true,
    timeFormat: String? = null
) :
    AbstractExporter(
        account, filter, notYetExportedP, dateFormat,
        decimalSeparator, encoding
    ) {
    private val timeFormatter: DateTimeFormatter? = timeFormat?.let { DateTimeFormatter.ofPattern(it)  }

    private var numberOfCategoryColumns = 2

    override fun export(
        context: Context,
        outputStream: Lazy<Result<DocumentFile>>,
        append: Boolean
    ): Result<Uri> {
        numberOfCategoryColumns = context.contentResolver.query(
            TransactionProvider.CATEGORIES_URI
                .buildUpon()
                .appendQueryParameter(TransactionProvider.QUERY_PARAMETER_HIERARCHICAL, "1")
                .build(),
            arrayOf("max(${DatabaseConstants.KEY_LEVEL})"),
            null, null, null
        )?.use {
            it.moveToFirst()
            it.getInt(0)
        } ?: numberOfCategoryColumns
        return super.export(context, outputStream, append)
    }

    override val format = ExportFormat.CSV
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
            if (splitCategoryLevels) {
                repeat(numberOfCategoryColumns) {
                    add(context.getString(R.string.category) + " " + (it +1))
                }
            } else {
                add(context.getString(R.string.category))
            }
            add(context.getString(R.string.comment))
            add(context.getString(R.string.method))
            add(context.getString(R.string.status))
            add(context.getString(R.string.reference_number))
            add(context.getString(R.string.picture))
            add(context.getString(R.string.tags))
        }
        StringBuilderWrapper().apply {
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

    private fun TransactionDTO.handleLabel(stringBuilderWrapper: StringBuilderWrapper) {
        with(stringBuilderWrapper) {
            if (splitCategoryLevels) {
                val path = catId?.let { categoryPaths[catId] }
                repeat(numberOfCategoryColumns) {
                    if (transferAccount != null) {
                        appendQ(if (it == 0) "[$transferAccount]" else "")
                    } else {
                        appendQ(path?.getOrNull(it) ?: "")
                    }
                    append(delimiter)
                }
            } else {
                appendQ(fullLabel(categoryPaths) ?: "")
                append(delimiter)
            }
        }
    }

    private fun TransactionDTO.handleAmount(stringBuilderWrapper: StringBuilderWrapper) {
        with(stringBuilderWrapper) {
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

    private fun TransactionDTO.handleDateTime(stringBuilderWrapper: StringBuilderWrapper) {
        with(stringBuilderWrapper) {
            appendQ(dateFormatter.format(date))
            append(delimiter)
            if (timeFormatter != null) {
                appendQ(timeFormatter.format(date))
                append(delimiter)
            }
        }
    }

    private fun TransactionDTO.handleTags(stringBuilderWrapper: StringBuilderWrapper) {
        with(stringBuilderWrapper) {
            appendQ(tagList?.joinToString(", ") { if (it.contains(',')) "'$it'" else it } ?: "")
        }
    }

    override fun TransactionDTO.marshall(categoryPaths: Map<Long, List<String>>) =
        StringBuilderWrapper().apply {
            if (withAccountColumn) {
                appendQ(account.label).append(delimiter)
            }
            val splitIndicator = if (splits != null) SplitTransaction.CSV_INDICATOR else ""
            appendQ(splitIndicator)
            append(delimiter)
            handleDateTime(this)
            appendQ(payee ?: "")
            append(delimiter)
            handleAmount(this)
            handleLabel(this)
            appendQ(comment ?: "")
            append(delimiter)
            appendQ(methodLabel ?: "")
            append(delimiter)
            appendQ(status?.symbol ?: "")
            append(delimiter)
            appendQ(referenceNumber ?: "")
            append(delimiter)
            appendQ(pictureFileName ?: "")
            append(delimiter)
            handleTags(this)
            splits?.forEach {
                append("\n")
                if (withAccountColumn) {
                    appendQ("").append(delimiter)
                }
                with(it) {
                    appendQ(SplitTransaction.CSV_PART_INDICATOR)
                    append(delimiter)
                    handleDateTime(this@apply)
                    appendQ(payee ?: "")
                    append(delimiter)
                    handleAmount(this@apply)
                    handleLabel(this@apply)
                    appendQ(comment ?: "")
                    append(delimiter)
                    appendQ("")
                    append(delimiter)
                    appendQ("")
                    append(delimiter)
                    appendQ("")
                    append(delimiter)
                    appendQ(pictureFileName ?: "")
                    append(delimiter)
                    handleTags(this@apply)
                }
            }
        }.toString()
}