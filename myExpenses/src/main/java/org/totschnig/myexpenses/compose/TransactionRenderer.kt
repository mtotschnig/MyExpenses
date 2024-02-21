package org.totschnig.myexpenses.compose

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.InlineTextContent
import androidx.compose.foundation.text.appendInlineContent
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.Placeholder
import androidx.compose.ui.text.PlaceholderVerticalAlign
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.viewmodel.data.Category.Companion.NO_CATEGORY_ASSIGNED_LABEL
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.text.Typography.ellipsis

val inlineIconPlaceholder = 13.sp
val inlineIconSize = 12.sp

enum class ColorSource { TYPE, SIGN }


abstract class ItemRenderer(
    private val withCategoryIcon: Boolean,
    private val colorSource: ColorSource,
    private val onToggleCrStatus: ((Long) -> Unit)?
) {

    fun Transaction2.buildPrimaryInfo(
        context: Context,
        forLegacy: Boolean
    ): AnnotatedString {
        return buildAnnotatedString {
            if (isSplit) {
                append(context.getString(R.string.split_transaction))
            } else if (forLegacy && !isTransfer && catId == null &&
                status != DatabaseConstants.STATUS_HELPER
            ) {
                append(NO_CATEGORY_ASSIGNED_LABEL)
            } else {
                categoryPath?.let {
                    if (forLegacy) {
                        append(it)
                    } else {
                        withStyle(style = SpanStyle(fontWeight = FontWeight.Bold)) {
                            append(it)
                        }
                    }
                }
                if (isTransfer) {
                    if (categoryPath != null) append(" (")
                    accountLabel?.let { append("$it ") }
                    if (forLegacy || accountLabel != null) {
                        append(Transfer.getIndicatorPrefixForLabel(amount.amountMinor))
                    }
                    transferAccountLabel?.let { append(it) }
                    if (categoryPath != null) append(")")
                }
            }
        }
    }

    fun Transaction2.buildSecondaryInfo(
        context: Context,
        withTags: Boolean
    ): Pair<AnnotatedString, List<String>> {
        val attachmentIcon = if (attachmentCount > 0) "paperclip" else null
        val methodInfo = getMethodInfo(context)
        val methodIcon = methodInfo?.second
        return buildAnnotatedString {
            methodIcon?.let {
                appendInlineContent(it, methodInfo.first)
                append(" ")
            }
            referenceNumber?.takeIf { it.isNotEmpty() }?.let {
                append("$it ")
            }
            comment?.takeIf { it.isNotEmpty() }?.let {
                withStyle(style = SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(
                        if (comment.length > 100)
                            comment.substring(0, 100) + ellipsis
                        else comment
                    )
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
            tagList.takeIf { withTags && it.isNotEmpty() }?.let { list ->
                if (length > 0) {
                    append(COMMENT_SEPARATOR)
                }
                list.forEachIndexed { index, pair ->
                    if (pair.second == null) {
                        append(pair.first)
                    } else {
                        val userColor = Color(pair.second!!)
                        withStyle(
                            style = SpanStyle(
                                background = userColor,
                                color = if (userColor.luminance() > 0.5) Color.Black else Color.White,
                            )
                        ) {
                            append(pair.first)
                        }
                    }
                    if (index < list.size - 1) {
                        append(" ")
                    }
                }
            }
            attachmentIcon?.let {
                append(" ")
                appendInlineContent(
                    it,
                    context.getString(R.string.content_description_attachment)
                )
                if (attachmentCount > 1) {
                    append("($attachmentCount)")
                }
            }
        } to listOfNotNull(methodIcon, attachmentIcon)
    }

    @Composable
    abstract fun RowScope.RenderInner(transaction: Transaction2)

    abstract fun Modifier.height(): Modifier

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(
        transaction: Transaction2,
        modifier: Modifier = Modifier,
        selectionHandler: SelectionHandler? = null,
        menuGenerator: (Transaction2) -> Menu? = { null }
    ) {
        val showMenu = remember { mutableStateOf(false) }
        val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)
        val voidMarkerHeight = with(LocalDensity.current) { 2.dp.toPx() }
        val voidStatus = stringResource(id = R.string.status_void)
        Row(modifier = modifier
            .height()
            .optional(selectionHandler,
                ifPresent = {
                    combinedClickable(
                        onLongClick = { it.toggle(transaction) },
                        onClick = {
                            if (it.selectionCount == 0) {
                                showMenu.value = true
                            } else {
                                it.toggle(transaction)
                            }
                        }
                    )
                },
                ifAbsent = {
                    clickable { showMenu.value = true }
                }
            )
            .conditional(selectionHandler?.isSelected(transaction) == true) {
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
                    HierarchicalMenu(showMenu, it)
                }
            }
        }

    }

    @Composable
    protected fun Transaction2.StatusToggle() {
        onToggleCrStatus?.let {
            val color = colorResource(id = crStatus.color)
            Box(modifier = Modifier
                .size(32.dp)
                .conditional(
                    (crStatus == CrStatus.UNRECONCILED || crStatus == CrStatus.CLEARED)
                            && accountType != AccountType.CASH
                ) {
                    clickable { it(id) }
                }
                .padding(8.dp)
                .conditional(crStatus != CrStatus.VOID && accountType != AccountType.CASH) {
                    background(color = color)
                }
            )
        }
    }

    @Composable
    protected fun Transaction2.CategoryIcon() {
        if (withCategoryIcon) {
            Box(modifier = Modifier.size(30.sp), contentAlignment = Alignment.Center) {
                when {
                    isSplit -> Icon(
                        imageVector = Icons.AutoMirrored.Filled.CallSplit,
                        contentDescription = stringResource(id = R.string.split_transaction),
                        modifier = Modifier.fillMaxSize()
                    )

                    icon != null -> Icon(icon)

                    isTransfer -> CharIcon(
                        char = if (accountLabel != null) '⬧' else Transfer.getIndicatorCharForLabel(
                            amount.amountMinor > 0
                        )
                    )

                    else -> Icon("minus")
                }
            }
        }
    }

    @Composable
    protected fun Transaction2.AccountColor() {
        color?.let {
            VerticalDivider(
                thickness = 2.dp,
                color = Color(it)
            )
            Spacer(modifier = Modifier.width(5.dp))
        }
    }

    @Composable
    fun TextWithInlineContent(
        modifier: Modifier = Modifier,
        text: AnnotatedString,
        icons: List<String>
    ) {
        Text(modifier = modifier, text = text, inlineContent = buildMap {
            icons.forEach {
                put(it, inlineIcon(it))
            }
        })
    }

    private fun inlineIcon(icon: String) = InlineTextContent(
        Placeholder(
            width = inlineIconPlaceholder,
            height = inlineIconPlaceholder,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        Icon(icon, size = inlineIconSize)
    }

    @Composable
    fun Transaction2.ColoredAmountText(
        style: TextStyle = LocalTextStyle.current,
    ) {
        ColoredAmountText(
            money = if (isTransferAggregate) amount.negate() else amount,
            style = style,
            type = when {
                isTransferAggregate -> FLAG_NEUTRAL

                colorSource == ColorSource.TYPE -> type

                else -> null
            }
        )
    }
}

class CompactTransactionRenderer(
    private val dateTimeFormatInfo: Pair<DateTimeFormatter, Dp>?,
    withCategoryIcon: Boolean = true,
    colorSource: ColorSource = ColorSource.TYPE,
    onToggleCrStatus: ((Long) -> Unit)? = null
) : ItemRenderer(withCategoryIcon, colorSource, onToggleCrStatus) {

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
        transaction.AccountColor()
        dateTimeFormatInfo?.let {
            Text(
                modifier = Modifier.width(it.second),
                text = it.first.format(transaction.date),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        transaction.StatusToggle()
        transaction.CategoryIcon()
        TextWithInlineContent(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f),
            text = description,
            icons = secondaryInfo.second
        )
        transaction.ColoredAmountText()
    }

    override fun Modifier.height() = this.height(IntrinsicSize.Min)
}

class NewTransactionRenderer(
    private val dateTimeFormatter: DateTimeFormatter?,
    withCategoryIcon: Boolean = true,
    colorSource: ColorSource = ColorSource.TYPE,
    onToggleCrStatus: ((Long) -> Unit)? = null
) : ItemRenderer(withCategoryIcon, colorSource, onToggleCrStatus) {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun RowScope.RenderInner(transaction: Transaction2) {
        val context = LocalContext.current
        val primaryInfo = transaction.buildPrimaryInfo(context, false)
        val secondaryInfo = transaction.buildSecondaryInfo(context, false)
        transaction.CategoryIcon()
        transaction.StatusToggle()
        Column(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f)
        ) {
            if (!transaction.isTransfer && transaction.accountLabel != null) {
                Row(modifier = Modifier.height(IntrinsicSize.Min)) {
                    transaction.AccountColor()
                    Text(text = transaction.accountLabel)
                }
            }
            primaryInfo.takeIf { it.isNotEmpty() }
                ?.let { info ->
                    Text(text = info)
                }
            secondaryInfo.first.takeIf { it.isNotEmpty() }?.let { info ->
                TextWithInlineContent(text = info, icons = secondaryInfo.second)
            }
            if (transaction.tagList.isNotEmpty()) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    transaction.tagList.forEach { (label, color) ->
                        InlineChip(text = label, color = color?.let { Color(it) })
                    }
                }
            }

        }
        Column(horizontalAlignment = Alignment.End) {
            transaction.ColoredAmountText(
                style = MaterialTheme.typography.bodyLarge,
            )
            dateTimeFormatter?.let {
                Text(text = it.format(transaction.date), style = MaterialTheme.typography.bodySmall)
            }
        }
    }

    override fun Modifier.height() = this.heightIn(min = 48.dp)
}

val Transaction2.isTransferAggregate
    get() = isTransfer && accountLabel != null

enum class RenderType {
    Legacy, New
}

fun Modifier.tagBorder(color: Color) =
    border(
        border = BorderStroke(1.5.dp, color),
        shape = RoundedCornerShape(8.dp),
    )
        .padding(vertical = 4.dp, horizontal = 6.dp)

@Preview
@Composable
fun RenderNew(@PreviewParameter(SampleProvider::class) transaction: Transaction2) {
    NewTransactionRenderer(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        .Render(transaction)
}

@Preview
@Composable
fun RenderCompact(@PreviewParameter(SampleProvider::class) transaction: Transaction2) {
    CompactTransactionRenderer(
        DateTimeFormatter.ofPattern("EEE") to 40.dp
    ).Render(transaction)
}

@Composable
fun InlineChip(text: String, color: Color?) {
    Text(
        text = text,
        modifier = Modifier
            .tagBorder(color ?: MaterialTheme.colorScheme.onSurface)
            .padding(bottom = 2.dp),
        style = MaterialTheme.typography.bodySmall
    )
}

class SampleProvider : PreviewParameterProvider<Transaction2> {
    override val values = sequenceOf(
        Transaction2(
            id = -1,
            _date = System.currentTimeMillis() / 1000,
            amount = Money(CurrencyUnit.DebugInstance, 7000),
            methodLabel = "CHEQUE",
            methodIcon = "credit-card",
            referenceNumber = "1",
            accountId = -1,
            catId = 1,
            categoryPath = "Obst und Gemüse",
            comment = "Erika Musterfrau",
            icon = "apple",
            year = 2022,
            month = 1,
            day = 1,
            week = 1,
            tagList = listOf(
                "Hund" to android.graphics.Color.RED,
                "Katz" to android.graphics.Color.GREEN
            )
        ),
        Transaction2(
            id = -1,
            _date = System.currentTimeMillis() / 1000,
            amount = Money(CurrencyUnit.DebugInstance, 7000),
            accountId = -1,
            catId = SPLIT_CATID,
            payee = "Erika Musterfrau",
            year = 2022,
            month = 1,
            day = 1,
            week = 1,
            tagList = listOf(
                "Hund" to android.graphics.Color.RED,
                "Katz" to android.graphics.Color.GREEN
            ),
            accountType = AccountType.BANK
        )
    )
}