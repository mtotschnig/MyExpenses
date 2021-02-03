package org.totschnig.myexpenses.export

import android.content.Context
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.provider.filter.WhereFilter
import org.totschnig.myexpenses.util.StringBuilderWrapper
import java.math.BigDecimal

class QifExporter(account: Account, filter: WhereFilter?, notYetExportedP: Boolean, dateFormat: String,
                  decimalSeparator: Char, encoding: String) :
        AbstractExporter(account, filter, notYetExportedP, dateFormat, decimalSeparator, encoding) {
    override val format = ExportFormat.QIF
    override fun header(context: Context) = StringBuilderWrapper().append("!Account\nN")
            .append(account.label)
            .append("\nT")
            .append(account.type.toQifName())
            .append("\n^\n!Type:")
            .append(account.type.toQifName())
            .append("\n").toString()

    override fun line(isSplit: Boolean, dateStr: String, payee: String, amount: BigDecimal, labelMain: String, labelSub: String, fullLabel: String, comment: String, methodLabel: String?, status: CrStatus, referenceNumber: String, pictureFileName: String, tagList: String) = StringBuilderWrapper().apply {
        append("D")
                .append(dateStr)
                .append("\nT")
                .append(nfFormat.format(amount))
        if (comment.isNotEmpty()) {
            append("\nM").append(comment)
        }
        if (fullLabel.isNotEmpty()) {
            append("\nL").append(fullLabel)
        }
        if (payee.isNotEmpty()) {
            append("\nP").append(payee)
        }
        if ("" != status.symbol) {
            append("\nC").append(status.symbol)
        }
        if (referenceNumber.isNotEmpty()) {
            append("\nN").append(referenceNumber)
        }
    }.toString()

    override fun split(dateStr: String, payee: String, amount: BigDecimal, labelMain: String, labelSub: String, fullLabel: String, comment: String, pictureFileName: String) = StringBuilderWrapper().apply {
        append("S").append(fullLabel)
        if (comment.isNotEmpty()) {
            append("\nE").append(comment)
        }
        append("\n$").append(nfFormat.format(amount))
    }.toString()

    override fun recordDelimiter() = "^\n"
}