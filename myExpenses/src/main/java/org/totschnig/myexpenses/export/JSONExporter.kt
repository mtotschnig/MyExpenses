package org.totschnig.myexpenses.export

import android.content.Context
import com.google.gson.Gson
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.TransactionDTO
import org.totschnig.myexpenses.provider.filter.WhereFilter

/**
 * @param account          Account to print
 * @param filter           only transactions matched by filter will be considered
 * @param notYetExportedP  if true only transactions not marked as exported will be handled
 * @param dateFormat       format that can be parsed by SimpleDateFormat class
 * @param decimalSeparator , or .
 * @param encoding         the string describing the desired character encoding.
 */
class JSONExporter(
    account: Account,
    filter: WhereFilter?,
    notYetExportedP: Boolean,
    dateFormat: String,
    decimalSeparator: Char,
    encoding: String,
    val gson: Gson
) :
    AbstractExporter(
        account, filter, notYetExportedP, dateFormat,
        decimalSeparator, encoding
    ) {
    override val format = ExportFormat.JSON

    override fun header(context: Context) = "["

    override fun TransactionDTO.marshall(): String = gson.toJson(this)

    override fun recordDelimiter(isLastLine: Boolean) = if (isLastLine) null else ","

    override fun footer(): String = "]"
}