package org.totschnig.myexpenses.compose

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.time.format.DateTimeFormatter

class TransactionRendererLegacy(
    val dateTimeFormatter: DateTimeFormatter?,
    val onToggleCrStatus: ((Long) -> Unit)?
): ItemRenderer {
    @Composable
    override fun RowScope.RenderInner(transaction: Transaction2) {
        val description = buildAnnotatedString {
            append(transaction.buildPrimaryInfo(LocalContext.current))
            transaction.buildSecondaryInfo().takeIf { it.isNotEmpty() }?.let {
                append(COMMENT_SEPARATOR)
                append(it)
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
                .weight(1f),
            text = description
        )
        ColoredAmountText(money = transaction.amount)
    }
}