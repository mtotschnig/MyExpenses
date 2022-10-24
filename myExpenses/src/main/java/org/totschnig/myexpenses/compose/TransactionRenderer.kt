package org.totschnig.myexpenses.compose

import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Divider
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

abstract class ItemRenderer(val onToggleCrStatus: ((Long) -> Unit)?) {

    fun Transaction2.buildPrimaryInfo(context: Context) = buildAnnotatedString {
        referenceNumber?.takeIf { it.isNotEmpty() }?.let {
            append("($it) ")
        }
        if (transferPeer != null) {
            accountLabel?.let { append("$it ") }
            append(Transfer.getIndicatorPrefixForLabel(amount.amountMinor))
            label?.let { append(it) }
        } else if (isSplit) {
            append(context.getString(R.string.split_transaction))
        } else if (catId == null && status != DatabaseConstants.STATUS_HELPER) {
            append(org.totschnig.myexpenses.viewmodel.data.Category.NO_CATEGORY_ASSIGNED_LABEL)
        } else {
            label?.let {
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(it)
                }
            }
        }
    }

    fun Transaction2.buildSecondaryInfo() = buildAnnotatedString {
        comment?.takeIf { it.isNotEmpty() }?.let {
            withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                append(it)
            }
        }
        payee?.takeIf { it.isNotEmpty() }?.let {
            append(COMMENT_SEPARATOR)
            withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                append(it)
            }
        }
        tagList?.takeIf { it.isNotEmpty() }?.let {
            append(COMMENT_SEPARATOR)
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                append(it)
            }
        }
    }

    @Composable
    abstract fun RowScope.RenderInner(transaction: Transaction2)

    abstract fun Modifier.height() : Modifier

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(
        modifier: Modifier,
        transaction: Transaction2,
        selectionHandler: SelectionHandler,
        menuGenerator: (Transaction2) -> Menu<Transaction2>?,
        futureCriterion: ZonedDateTime
    ) {
        val showMenu = remember { mutableStateOf(false) }
        val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)
        val voidMarkerHeight = with(LocalDensity.current) { 2.dp.toPx() }
        val futureBackgroundColor = colorResource(id = R.color.future_background)
        val voidStatus = stringResource(id = R.string.status_void)
        Row(modifier = modifier
            .conditional(transaction.date >= futureCriterion) {
                background(futureBackgroundColor)
            }
            .height()
            .combinedClickable(
                onLongClick = { selectionHandler.toggle(transaction) },
                onClick = {
                    if (selectionHandler.selectionCount == 0) {
                        showMenu.value = true
                    } else {
                        selectionHandler.toggle(transaction)
                    }
                }
            )
            .conditional(selectionHandler.isSelected(transaction)) {
                background(activatedBackgroundColor)
            }
            .conditional(transaction.crStatus == CrStatus.VOID) {
                drawWithContent {
                    drawContent()
                    drawLine(
                        Color.Red,
                        Offset(0F, size.height / 2),
                        Offset(size.width, size.height / 2),
                        voidMarkerHeight
                    )
                }
                    .semantics { contentDescription = voidStatus }
            }
            .padding(horizontal = mainScreenPadding, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RenderInner(transaction = transaction)
            if (showMenu.value) {
                remember { menuGenerator(transaction) }?.let {
                    HierarchicalMenu(showMenu, it, transaction)
                }
            }
        }

    }

    @Composable
    protected fun StatusToggle(transaction: Transaction2) {
        onToggleCrStatus?.let {
            if (transaction.crStatus != CrStatus.VOID) {
                Box(modifier = Modifier
                    .size(32.dp)
                    .clickable { it(transaction.id) }
                    .padding(8.dp)
                    .background(color = Color(transaction.crStatus.color)))
            }
        }
    }
}

class TransactionRendererLegacy(
    val dateTimeFormatter: DateTimeFormatter?,
    onToggleCrStatus: ((Long) -> Unit)?
): ItemRenderer(onToggleCrStatus) {
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
            Text(text = it.format(transaction.date))
        }
        StatusToggle(transaction)
        Text(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f),
            text = description
        )
        ColoredAmountText(money = transaction.amount)
    }

    override fun Modifier.height() = this.height(IntrinsicSize.Min)
}

class NewTransactionRenderer(
    val dateTimeFormatter: DateTimeFormatter?,
    onToggleCrStatus: ((Long) -> Unit)?
) : ItemRenderer(onToggleCrStatus) {
    @Composable
    override fun RowScope.RenderInner(transaction: Transaction2) {
        transaction.icon?.let { Icon(icon = it) }
        StatusToggle(transaction = transaction)
        Column(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f)
        ) {
            Text(text = transaction.buildPrimaryInfo(LocalContext.current))
            transaction.buildSecondaryInfo().takeIf { it.isNotEmpty() }?.let {
                Text(text = it)
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            ColoredAmountText(money = transaction.amount)
            dateTimeFormatter?.let {
                Text(text = it.format(transaction.date))
            }
        }
    }

    override fun Modifier.height() = this.heightIn(min = 48.dp)
}

enum class RenderType {
    legacy, new
}