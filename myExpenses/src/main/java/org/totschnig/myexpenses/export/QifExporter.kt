package org.totschnig.myexpenses.export

import android.content.Context
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model.ExportFormat
import org.totschnig.myexpenses.model.TransactionDTO
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.provider.filter.Criterion

class QifExporter(
    account: Account,
    currencyContext: CurrencyContext,
    filter: Criterion?,
    notYetExportedP: Boolean,
    dateFormat: String,
    decimalSeparator: Char,
    encoding: String
) :
    AbstractExporter(account, currencyContext, filter, notYetExportedP, dateFormat, decimalSeparator, encoding) {
    override val format = ExportFormat.QIF
    override fun header(context: Context) = StringBuilder().append("!Account\nN")
        .append(account.label)
        .append("\nT")
        .append(account.type.qifName)
        .append("\n^\n!Type:")
        .append(account.type.qifName)
        .append("\n").toString()

    override fun TransactionDTO.marshall(categoryPaths: Map<Long, List<String>>) = StringBuilder().apply {
        append("D")
            .append(dateFormatter.format(date))
            .append("\nT")
            .append(nfFormat.format(amount))
        comment?.takeIf { it.isNotEmpty() }?.let {
            append("\nM").append(it.escapeNewLine())
        }
        fullLabel(categoryPaths)?.takeIf { it.isNotEmpty() }?.let {
            append("\nL").append(it)
        }
        payee?.takeIf { it.isNotEmpty() }?.let {
            append("\nP").append(it)
        }
        status?.symbol?.let {
            append("\nC").append(it)
        }
        referenceNumber?.takeIf { it.isNotEmpty() }?.let {
            append("\nN").append(it)
        }

        splits?.forEach { split ->
            append("\n").append("S").append(split.fullLabel(categoryPaths))
            split.comment?.takeIf { it.isNotEmpty() }?.let {
                append("\nE").append(it)
            }
            append("\n$").append(nfFormat.format(split.amount))
        }
    }.toString()

    override fun recordDelimiter(isLastLine: Boolean): String = "\n^\n"
}