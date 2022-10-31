package org.totschnig.myexpenses.compose

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Attachment
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.accompanist.flowlayout.FlowRow
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

abstract class ItemRenderer(private val onToggleCrStatus: ((Long) -> Unit)?) {

    fun Transaction2.buildPrimaryInfo(
        context: Context,
        forLegacy: Boolean
    ): AnnotatedString {
        return buildAnnotatedString {
            if (isTransfer) {
                accountLabel?.let { append("$it ") }
                append(Transfer.getIndicatorPrefixForLabel(amount.amountMinor))
                label?.let { append(it) }
            } else if (isSplit) {
                append(context.getString(R.string.split_transaction))
            } else if (forLegacy && catId == null && status != DatabaseConstants.STATUS_HELPER) {
                append(org.totschnig.myexpenses.viewmodel.data.Category.NO_CATEGORY_ASSIGNED_LABEL)
            } else {
                label?.let {
                    withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                        append(it)
                    }
                }
            }
        }
    }

    fun Transaction2.buildSecondaryInfo(
        context: Context,
        withTags: Boolean
    ): Pair<AnnotatedString, List<ImageVector>> {
        val attachmentIcon = if (pictureUri != null) Icons.Filled.Attachment else null
        val methodIcon = methodInfo?.second
        return buildAnnotatedString {
            methodIcon?.let {
                appendInlineContent(it.name, methodInfo!!.first)
            }
            referenceNumber?.takeIf { it.isNotEmpty() }?.let {
                append("($it) ")
            }
            comment?.takeIf { it.isNotEmpty() }?.let {
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(it)
                }
            }
            payee?.takeIf { it.isNotEmpty() }?.let {
                if (length > 0) {
                    append(COMMENT_SEPARATOR)
                }
                withStyle(style = SpanStyle(textDecoration = TextDecoration.Underline)) {
                    append(it)
                }
            }
            tagList.takeIf { withTags && it.isNotEmpty() }?.let {
                if (length > 0) {
                    append(COMMENT_SEPARATOR)
                }
                withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(it.joinToString())
                }
            }
            attachmentIcon?.let {
                append(" ")
                appendInlineContent(
                    it.name,
                    context.getString(R.string.content_description_attachment)
                )
            }
        } to listOfNotNull(methodIcon, attachmentIcon)
    }

    @Composable
    abstract fun RowScope.RenderInner(transaction: Transaction2)

    abstract fun Modifier.height(): Modifier

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(
        modifier: Modifier = Modifier,
        transaction: Transaction2,
        selectionHandler: SelectionHandler,
        menuGenerator: (Transaction2) -> Menu<Transaction2>?
    ) {
        val showMenu = remember { mutableStateOf(false) }
        val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)
        val voidMarkerHeight = with(LocalDensity.current) { 2.dp.toPx() }
        val voidStatus = stringResource(id = R.string.status_void)
        Row(modifier = modifier
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

    @Composable
    fun TextWithInlineContent(
        modifier: Modifier = Modifier,
        text: AnnotatedString,
        icons: List<ImageVector>
    ) {
        Text(modifier = modifier, text = text, inlineContent = buildMap {
            icons.forEach {
                put(it.name, inlineIcon(it))
            }
        })
    }

    private fun inlineIcon(icon: ImageVector) = InlineTextContent(
        Placeholder(
            width = 12.sp,
            height = 12.sp,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        androidx.compose.material.Icon(imageVector = icon, contentDescription = icon.name)
    }
}

class LegacyTransactionRenderer(
    private val dateTimeFormatter: DateTimeFormatter?,
    onToggleCrStatus: ((Long) -> Unit)?
) : ItemRenderer(onToggleCrStatus) {
    @Composable
    override fun RowScope.RenderInner(transaction: Transaction2) {
        val context = LocalContext.current
        val secondaryInfo = transaction.buildSecondaryInfo(context, true)
        val description = buildAnnotatedString {
            append(transaction.buildPrimaryInfo(context, true))
            secondaryInfo.first.takeIf { it.isNotEmpty() }?.let {
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
        TextWithInlineContent(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f),
            text = description,
            icons = secondaryInfo.second
        )
        ColoredAmountText(money = transaction.amount)
    }

    override fun Modifier.height() = this.height(IntrinsicSize.Min)
}

class NewTransactionRenderer(
    private val dateTimeFormatter: DateTimeFormatter?,
    onToggleCrStatus: ((Long) -> Unit)?
) : ItemRenderer(onToggleCrStatus) {
    @Composable
    override fun RowScope.RenderInner(transaction: Transaction2) {
        val context = LocalContext.current
        val primaryInfo = transaction.buildPrimaryInfo(context, false)
        val secondaryInfo = transaction.buildSecondaryInfo(context, false)
        if (
            transaction.isSplit ||
            (transaction.isTransfer && transaction.accountLabel == null) ||
            transaction.icon != null ||
            //if there is no information at all for the transaction, we want to render the minus icon
            (primaryInfo.isEmpty() && secondaryInfo.first.isEmpty() && transaction.tagList.isEmpty()) ) {
            Box(modifier = Modifier.size(30.sp), contentAlignment = Alignment.Center) {
                when {
                    transaction.isSplit -> androidx.compose.material.Icon(
                        imageVector = Icons.Filled.CallSplit,
                        contentDescription = stringResource(id = R.string.split_transaction),
                        modifier = Modifier.fillMaxSize()
                    )
                    transaction.isTransfer -> Icon("money-bill-transfer")
                    else -> Icon(transaction.icon ?: "minus")
                }
            }
        }
        StatusToggle(transaction = transaction)
        Column(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f)
        ) {
            primaryInfo.takeIf { it.isNotEmpty() }
                ?.let { info ->
                    Text(text = info)
                }
            secondaryInfo.first.takeIf { it.isNotEmpty() }?.let { info ->
                TextWithInlineContent(text = info, icons = secondaryInfo.second)
            }
            if (transaction.tagList.isNotEmpty()) {
                FlowRow(mainAxisSpacing = 2.dp, crossAxisSpacing = 1.dp) {
                    transaction.tagList.forEach {
                        Text(text = it, modifier = Modifier.tagBorder())
                    }
                }
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
    Legacy, New
}

fun Modifier.tagBorder() = composed {
    border(
        border = BorderStroke(
            ButtonDefaults.OutlinedBorderSize,
            MaterialTheme.colors.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
    )
        .padding(horizontal = 4.dp)
}

@Preview
@Composable
fun RenderNew(@PreviewParameter(SampleProvider::class) transaction: Transaction2) {
    NewTransactionRenderer(null, null).Render(
        transaction = transaction,
        selectionHandler = object : SelectionHandler {
            override fun toggle(transaction: Transaction2) {}

            override fun isSelected(transaction: Transaction2) = false

            override val selectionCount: Int = 0
        },
        menuGenerator = { null }
    )
}

@Preview
@Composable
fun RenderLegacy(@PreviewParameter(SampleProvider::class) transaction: Transaction2) {
    LegacyTransactionRenderer(null, null).Render(
        transaction = transaction,
        selectionHandler = object : SelectionHandler {
            override fun toggle(transaction: Transaction2) {}

            override fun isSelected(transaction: Transaction2) = false

            override val selectionCount: Int = 0
        },
        menuGenerator = { null }
    )
}

class SampleProvider : PreviewParameterProvider<Transaction2> {
    override val values = sequenceOf(
        Transaction2(
            id = -1,
            date = ZonedDateTime.now(),
            amount = Money(CurrencyUnit.DebugInstance, 7000),
            methodLabel = "CHEQUE",
            referenceNumber = "1",
            accountId = -1,
            catId = 1,
            label = "Obst und Gemüse",
            comment = "Erika Musterfrau",
            icon = "apple",
            year = 2022,
            month = 1,
            day = 1,
            week = 1,
            tagList = listOf("Hund", "Katz"),
            pictureUri = Uri.EMPTY
        ),
        Transaction2(
            id = -1,
            date = ZonedDateTime.now(),
            amount = Money(CurrencyUnit.DebugInstance, 7000),
            accountId = -1,
            catId = SPLIT_CATID,
            payee = "Erika Musterfrau",
            year = 2022,
            month = 1,
            day = 1,
            week = 1,
            tagList = listOf("Hund", "Katz")
        )
    )
}