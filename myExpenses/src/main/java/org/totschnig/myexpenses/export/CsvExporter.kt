package org.totschnig.myexpenses.export

import android.content.Context
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.SplitTransaction
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.StringBuilderWrapper
import java.math.BigDecimal

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
class CsvExporter(account: Account, filter: WhereFilter?,
                  notYetExportedP: Boolean, dateFormat: String,
                  decimalSeparator: Char, encoding: String, private val withHeader: Boolean, val delimiter: Char,
                  private val withAccountColumn: Boolean) :
        AbstractExporter(account, filter, notYetExportedP, dateFormat,
                decimalSeparator, encoding) {
    override val format = ExportFormat.CSV
    override fun header(context: Context) = if (withHeader) {
        val columns = intArrayOf(R.string.split_transaction, R.string.date, R.string.payer_or_payee, R.string.income, R.string.expense,
                R.string.category, R.string.subcategory, R.string.comment, R.string.method, R.string.status, R.string.reference_number, R.string.picture, R.string.tags)
        StringBuilderWrapper().apply {
            if (withAccountColumn) {
                appendQ(context.getString(R.string.account)).append(delimiter)
            }
            for (column in columns) {
                appendQ(context.getString(column)).append(delimiter)
            }
            append("\n")
        }.toString()
    } else null

    override fun line(isSplit: Boolean, dateStr: String, payee: String, amount: BigDecimal, labelMain: String, labelSub: String, fullLabel: String, comment: String, methodLabel: String?, status: CrStatus, referenceNumber: String, pictureFileName: String, tagList: String) =
            StringBuilderWrapper().apply {
                if (withAccountColumn) {
                    appendQ(account.label).append(delimiter)
                }
                val splitIndicator = if (isSplit) SplitTransaction.CSV_INDICATOR else ""
                val amountAbsCSV = nfFormat.format(amount.abs())
                appendQ(splitIndicator)
                        .append(delimiter)
                        .appendQ(dateStr)
                        .append(delimiter)
                        .appendQ(payee)
                        .append(delimiter)
                        .appendQ((if (amount.signum() == 1) amountAbsCSV else "0"))
                        .append(delimiter)
                        .appendQ((if (amount.signum() == -1) amountAbsCSV else "0"))
                        .append(delimiter)
                        .appendQ(labelMain)
                        .append(delimiter)
                        .appendQ(labelSub)
                        .append(delimiter)
                        .appendQ(comment)
                        .append(delimiter)
                        .appendQ(methodLabel ?: "")
                        .append(delimiter)
                        .appendQ(status.symbol)
                        .append(delimiter)
                        .appendQ(referenceNumber)
                        .append(delimiter)
                        .appendQ(pictureFileName)
                        .append(delimiter)
                        .appendQ(tagList)
            }.toString()

    override fun split(dateStr: String, payee: String, amount: BigDecimal, labelMain: String, labelSub: String, fullLabel: String, comment: String, pictureFileName: String) =
            StringBuilderWrapper().apply {
                if (withAccountColumn) {
                    appendQ("").append(delimiter)
                }
                val amountAbsCSV = nfFormat.format(amount.abs())
                appendQ(SplitTransaction.CSV_PART_INDICATOR)
                        .append(delimiter)
                        .appendQ(dateStr)
                        .append(delimiter)
                        .appendQ(payee)
                        .append(delimiter)
                        .appendQ((if (amount.signum() == 1) amountAbsCSV else "0"))
                        .append(delimiter)
                        .appendQ((if (amount.signum() == -1) amountAbsCSV else "0"))
                        .append(delimiter)
                        .appendQ(labelMain)
                        .append(delimiter)
                        .appendQ(labelSub)
                        .append(delimiter)
                        .appendQ(comment)
                        .append(delimiter)
                        .appendQ("")
                        .append(delimiter)
                        .appendQ("")
                        .append(delimiter)
                        .appendQ("")
                        .append(delimiter)
                        .appendQ(pictureFileName)
                        .append(delimiter)
                        .appendQ("")
            }.toString()

    }