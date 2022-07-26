package org.totschnig.myexpenses.export

import android.content.Context
import android.net.Uri
import android.os.Bundle
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
    private val withAccountColumn: Boolean
) :
    AbstractExporter(
        account, filter, notYetExportedP, dateFormat,
        decimalSeparator, encoding
    ) {
    var numberOfCategoryColumns = 2

    companion object {
        const val KEY_SPLIT_CATEGORY_LEVELS = "split_category_levels"
    }

    override fun export(
        context: Context,
        outputStream: Lazy<Result<DocumentFile>>,
        append: Boolean,
        options: Bundle
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
        return super.export(context, outputStream, append, options)
    }

    override val format = ExportFormat.CSV
    override fun header(context: Context, options: Bundle) = if (withHeader) {
        val columns = buildList {
            add(context.getString(R.string.split_transaction))
            add(context.getString(R.string.date))
            add(context.getString(R.string.payer_or_payee))
            add(context.getString(R.string.income))
            add(context.getString(R.string.expense))
            if (options.getBoolean(KEY_SPLIT_CATEGORY_LEVELS)) {
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

    private fun TransactionDTO.handleLabel(
        stringBuilderWrapper: StringBuilderWrapper,
        splitLevels: Boolean
    ) {
        with(stringBuilderWrapper) {
            if (splitLevels) {
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

    override fun TransactionDTO.marshall(options: Bundle, categoryPaths: Map<Long, List<String>>) =
        StringBuilderWrapper().apply {
            if (withAccountColumn) {
                appendQ(account.label).append(delimiter)
            }
            val splitIndicator = if (splits != null) SplitTransaction.CSV_INDICATOR else ""
            val amountAbsCSV = nfFormat.format(amount.abs())
            appendQ(splitIndicator)
            append(delimiter)
            appendQ(dateStr)
            append(delimiter)
            appendQ(payee)
            append(delimiter)
            appendQ((if (amount.signum() == 1) amountAbsCSV else "0"))
            append(delimiter)
            appendQ((if (amount.signum() == -1) amountAbsCSV else "0"))
            append(delimiter)
            handleLabel(this, options.getBoolean(KEY_SPLIT_CATEGORY_LEVELS))
            appendQ(comment)
            append(delimiter)
            appendQ(methodLabel ?: "")
            append(delimiter)
            appendQ(status?.symbol ?: "")
            append(delimiter)
            appendQ(referenceNumber ?: "")
            append(delimiter)
            appendQ(pictureFileName ?: "")
            append(delimiter)
            appendQ(tagList ?: "")
            splits?.forEach {
                append("\n")
                if (withAccountColumn) {
                    appendQ("").append(delimiter)
                }
                with(it) {
                    val amountAbsCSV = nfFormat.format(amount.abs())
                    appendQ(SplitTransaction.CSV_PART_INDICATOR)
                    append(delimiter)
                    appendQ(dateStr)
                    append(delimiter)
                    appendQ(payee)
                    append(delimiter)
                    appendQ((if (amount.signum() == 1) amountAbsCSV else "0"))
                    append(delimiter)
                    appendQ((if (amount.signum() == -1) amountAbsCSV else "0"))
                    append(delimiter)
                    handleLabel(this@apply, options.getBoolean(KEY_SPLIT_CATEGORY_LEVELS))
                    appendQ(comment)
                    append(delimiter)
                    appendQ("")
                    append(delimiter)
                    appendQ("")
                    append(delimiter)
                    appendQ("")
                    append(delimiter)
                    appendQ(pictureFileName ?: "")
                    append(delimiter)
                    appendQ("")
                }
            }
        }.toString()
}