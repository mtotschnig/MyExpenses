package org.totschnig.myexpenses.export

import android.content.Context
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.TransactionDTO
import org.totschnig.myexpenses.provider.filter.WhereFilter
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
class JSONExporter(account: Account, filter: WhereFilter?,
                   notYetExportedP: Boolean, dateFormat: String,
                   decimalSeparator: Char, encoding: String, val delimiter: Char,
                   private val withAccountColumn: Boolean) :
        AbstractExporter(account, filter, notYetExportedP, dateFormat,
                decimalSeparator, encoding) {
    override val format = ExportFormat.CSV
    override fun header(context: Context) = "["
    override fun line(id: String, isSplit: Boolean, dateStr: String, payee: String, amount: BigDecimal, labelMain: String, labelSub: String, fullLabel: String, comment: String, methodLabel: String?, status: CrStatus, referenceNumber: String, pictureFileName: String, tagList: String) =
            TransactionDTO(id, isSplit, dateStr, payee, amount, labelMain, labelSub, fullLabel, comment, methodLabel,
                    status, referenceNumber, pictureFileName, tagList).toString()

    override fun split(dateStr: String, payee: String, amount: BigDecimal, labelMain: String, labelSub: String, fullLabel: String, comment: String, pictureFileName: String) =
            TransactionDTO(null, false, dateStr, payee, amount, labelMain, labelSub, fullLabel, comment,
                    null,
                    CrStatus.VOID, null, pictureFileName, null).toString()

    override fun recordDelimiter(): String? = ","
    override fun footer(): String? = "]"
}