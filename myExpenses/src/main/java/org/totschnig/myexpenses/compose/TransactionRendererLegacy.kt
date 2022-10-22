package org.totschnig.myexpenses.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.time.format.DateTimeFormatter

class TransactionRendererLegacy(
    val dateTimeFormatter: DateTimeFormatter?,
    val onToggleCrStatus: ((Long) -> Unit)?
): ItemRenderer {
    @Composable
    override fun RowScope.RenderInner(transaction: Transaction2) {
        val description = buildAnnotatedString {
            transaction.referenceNumber?.takeIf { it.isNotEmpty() }?.let {
                append("($it) ")
            }
            if (transaction.transferPeer != null) {
                transaction.accountLabel?.let { append("$it ") }
                append(Transfer.getIndicatorPrefixForLabel(transaction.amount.amountMinor))
                transaction.label?.let { append(it) }
            } else if (transaction.isSplit) {
                append(stringResource(id = R.string.split_transaction))
            } else if (transaction.catId == null && transaction.status != DatabaseConstants.STATUS_HELPER) {
                append(org.totschnig.myexpenses.viewmodel.data.Category.NO_CATEGORY_ASSIGNED_LABEL)
            } else {
                transaction.label?.let { append(it) }
            }
            transaction.comment?.takeIf { it.isNotEmpty() }?.let {
                append(COMMENT_SEPARATOR)
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(it)
                }
            }
            transaction.payee?.takeIf { it.isNotEmpty() }?.let {
                append(COMMENT_SEPARATOR)
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(it)
                }
            }
            transaction.tagList?.takeIf { it.isNotEmpty() }?.let {
                append(COMMENT_SEPARATOR)
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(it)
                }
            }
        }
        transaction.color?.let {
            Divider(
                color = Color(it),
                modifier = Modifier
                    .fillMaxHeight()
                    .width(2.dp)
            )
        }
        dateTimeFormatter?.let {
            Text(text = dateTimeFormatter.format(transaction.date))
        }
        onToggleCrStatus?.let {
            if (transaction.crStatus != CrStatus.VOID) {
                Box(modifier = Modifier
                    .size(32.dp)
                    .clickable { it(transaction.id) }
                    .padding(8.dp)
                    .background(color = Color(transaction.crStatus.color)))
            }
        }
        Text(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f), text = description
        )
        ColoredAmountText(money = transaction.amount)
    }
}