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
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import org.totschnig.myexpenses.model.CrStatus
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.Transfer
import org.totschnig.myexpenses.preference.ColorSource
import org.totschnig.myexpenses.provider.DatabaseConstants.SPLIT_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.STATUS_ARCHIVE
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.text.Typography.ellipsis

val inlineIconSize = 13.sp

abstract class ItemRenderer(
    private val withCategoryIcon: Boolean,
    private val colorSource: ColorSource,
    private val onToggleCrStatus: ((Long) -> Unit)?
) {

    fun Transaction2.buildPrimaryInfo(
        context: Context,
        resolvedSplitInfoLabels: String?,
        forLegacy: Boolean,
    ) = buildAnnotatedString {
        if (isSplit && resolvedSplitInfoLabels == null) {
            append(context.getString(R.string.split_transaction))
        } else {
            (resolvedSplitInfoLabels ?: categoryPath)?.let {
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
                    append(Transfer.getIndicatorPrefixForLabel(displayAmount.amountMinor))
                }
                transferAccountLabel?.let { append(it) }
                if (categoryPath != null) append(")")
            }
        }
    }

    fun Transaction2.buildSecondaryInfo(
        context: Context,
        withTags: Boolean,
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
            party?.displayName?.let {
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
                    if (pair.third == null) {
                        append(pair.second)
                    } else {
                        val userColor = Color(pair.third!!)
                        withStyle(
                            style = SpanStyle(
                                background = userColor,
                                color = if (userColor.luminance() > 0.5) Color.Black else Color.White,
                            )
                        ) {
                            append(pair.second)
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
    abstract fun RowScope.RenderInner(
        transaction: Transaction2,
        resolvedSplitInfoLabels: String? = null,
        resolvedSplitInfoIcons: List<String>? = null
    )

    abstract fun Modifier.height(): Modifier

    @OptIn(ExperimentalFoundationApi::class)
    @Composable
    fun Render(
        transaction: Transaction2,
        modifier: Modifier = Modifier,
        selectionHandler: SelectionHandler? = null,
        menuGenerator: (Transaction2) -> Menu? = { null },
        resolvedSplitInfo: List<Pair<String , String?>>? = null,
    ) {
        val showMenu = rememberSaveable { mutableStateOf(false) }
        val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)
        Row(
            modifier = modifier
                .height()
                .conditional(
                    selectionHandler?.isSelectable(transaction) == true,
                    ifTrue = {
                        combinedClickable(
                            onLongClick = { selectionHandler!!.toggle(transaction) },
                            onClick = {
                                if (selectionHandler!!.selectionCount == 0) {
                                    showMenu.value = true
                                } else {
                                    selectionHandler.toggle(transaction)
                                }
                            }
                        )
                    },
                    ifFalse = {
                        clickable { showMenu.value = true }
                    }
                )
                .conditional(selectionHandler?.isSelected(transaction) == true) {
                    background(activatedBackgroundColor)
                }
                .voidMarker(transaction.crStatus)
                .padding(horizontal = mainScreenPadding, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RenderInner(
                transaction = transaction,
                resolvedSplitInfoLabels = resolvedSplitInfo?.joinToString { it.first },
                resolvedSplitInfoIcons = resolvedSplitInfo?.mapNotNull { it.second }
            )
            menuGenerator(transaction)?.let {
                HierarchicalMenu(showMenu, it)
            }
        }
    }

    @Composable
    protected fun Transaction2.StatusToggle() {
        onToggleCrStatus?.let { toggle ->
            val color = colorResource(id = crStatus.color)
            val contentDescription = stringResource(crStatus.toStringRes())
            val onClickLabel = when (crStatus) {
                CrStatus.UNRECONCILED -> stringResource(R.string.mark_as_cleared)
                CrStatus.CLEARED -> stringResource(R.string.mark_as_unreconciled)
                else -> null
            }
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .optional(onClickLabel) {
                        clickable(onClickLabel = it) { toggle(id) }
                    }
                    .padding(8.dp)
                    .conditional(crStatus != CrStatus.VOID) {
                        background(color = color)
                            .semantics {
                                this.contentDescription = contentDescription
                            }
                    }
            )
        }
    }

    @Composable
    protected fun Transaction2.CategoryIcon(resolvedSplitIcons: List<String>?) {
        if (withCategoryIcon) {
            Box(modifier = Modifier.size(30.sp), contentAlignment = Alignment.Center) {
                when {
                    isSplit -> if (resolvedSplitIcons?.isNotEmpty() == true) {
                        Icon(resolvedSplitIcons[0], modifier = Modifier.align(Alignment.TopStart).fillMaxSize(0.5f), size = null)
                        resolvedSplitIcons.getOrNull(1)?.let {
                            Icon(it, modifier = Modifier.align(Alignment.TopEnd).fillMaxSize(0.5f), size = null)
                        }
                        resolvedSplitIcons.getOrNull(2)?.let {
                            Icon(it, modifier = Modifier.align(Alignment.BottomStart).fillMaxSize(0.5f), size = null)
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = stringResource(id = R.string.split_transaction),
                            modifier = Modifier.align(Alignment.BottomEnd).fillMaxSize(0.5f)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.CallSplit,
                            contentDescription = stringResource(id = R.string.split_transaction),
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    icon != null -> Icon(icon)

                    isTransfer -> CharIcon(
                        char = if (accountLabel != null) '⬧' else Transfer.getIndicatorCharForLabel(
                            displayAmount.amountMinor > 0
                        )
                    )

                    status == STATUS_ARCHIVE -> Icon(
                        imageVector = Icons.Filled.Archive,
                        contentDescription = stringResource(id = R.string.action_archive),
                        modifier = Modifier.fillMaxSize()
                    )

                    else -> Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize()
                    )
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
        icons: List<String>,
    ) {
        Text(modifier = modifier, text = text, inlineContent = buildMap {
            icons.forEach {
                put(it, inlineIcon(it))
            }
        })
    }

    private fun inlineIcon(icon: String) = InlineTextContent(
        Placeholder(
            width = inlineIconSize,
            height = inlineIconSize,
            placeholderVerticalAlign = PlaceholderVerticalAlign.TextCenter
        )
    ) {
        Icon(icon, size = inlineIconSize)
    }

    @Composable
    fun Transaction2.ColoredAmountText(
        style: TextStyle = LocalTextStyle.current,
        displayAmount: Money = this.displayAmount,
    ) {
        ColoredAmountText(
            money = if (type == FLAG_NEUTRAL) displayAmount.absolute() else displayAmount,
            style = style,
            type = colorSource.transformType(type)
        )
    }
}

data class DateTimeFormatInfo(
    val dateTimeFormatter: DateTimeFormatter,
    val emSize: Float,
)

class CompactTransactionRenderer(
    private val dateTimeFormatInfo: DateTimeFormatInfo?,
    withCategoryIcon: Boolean = true,
    private val withOriginalAmount: Boolean = false,
    colorSource: ColorSource = ColorSource.TYPE,
    onToggleCrStatus: ((Long) -> Unit)? = null,
) : ItemRenderer(withCategoryIcon, colorSource, onToggleCrStatus) {

    @Composable
    override fun RowScope.RenderInner(
        transaction: Transaction2,
        resolvedSplitInfoLabels: String?,
        resolvedSplitInfoIcons: List<String>?
    ) {
        val context = LocalContext.current
        val secondaryInfo = transaction.buildSecondaryInfo(context, true)
        val description = buildAnnotatedString {
            val primaryInfo = transaction.buildPrimaryInfo(context, resolvedSplitInfoLabels, true)
            if (primaryInfo.isNotEmpty()) {
                append(primaryInfo)
                if (secondaryInfo.first.isNotEmpty()) {
                    append(COMMENT_SEPARATOR)
                }
            }
            secondaryInfo.first.takeIf { it.isNotEmpty() }?.let {
                append(it)
            }
        }
        transaction.AccountColor()
        dateTimeFormatInfo?.let {
            Text(
                modifier = Modifier.width(emToDp(it.emSize)),
                text = it.dateTimeFormatter.format(transaction.date),
                textAlign = TextAlign.Center,
                maxLines = 1
            )
        }
        transaction.StatusToggle()
        transaction.CategoryIcon(resolvedSplitInfoIcons)
        TextWithInlineContent(
            modifier = Modifier
                .padding(horizontal = 5.dp)
                .weight(1f),
            text = description,
            icons = secondaryInfo.second
        )
        Column(horizontalAlignment = Alignment.End) {
            if (withOriginalAmount) {
                transaction.originalAmount?.let { transaction.ColoredAmountText(displayAmount = it) }
            }
            transaction.amount?.let { transaction.ColoredAmountText(displayAmount = it) }
            transaction.ColoredAmountText()
        }
    }

    override fun Modifier.height() = this.height(IntrinsicSize.Min)
}

class NewTransactionRenderer(
    private val dateTimeFormatter: DateTimeFormatter?,
    withCategoryIcon: Boolean = true,
    colorSource: ColorSource = ColorSource.TYPE,
    onToggleCrStatus: ((Long) -> Unit)? = null,
) : ItemRenderer(withCategoryIcon, colorSource, onToggleCrStatus) {
    @OptIn(ExperimentalLayoutApi::class)
    @Composable
    override fun RowScope.RenderInner(
        transaction: Transaction2,
        resolvedSplitInfoLabels: String?,
        resolvedSplitInfoIcons: List<String>?
    ) {
        val context = LocalContext.current
        val primaryInfo = transaction.buildPrimaryInfo(context, resolvedSplitInfoLabels, false)
        val secondaryInfo = transaction.buildSecondaryInfo(context, false)
        transaction.CategoryIcon(resolvedSplitInfoIcons)
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
                    transaction.tagList.forEach { (_, label, color) ->
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

enum class RenderType {
    Legacy, New
}

fun Modifier.tagBorder(color: Color) =
    border(
        border = BorderStroke(1.5.dp, color),
        shape = RoundedCornerShape(8.dp),
    )
        .padding(vertical = 4.dp, horizontal = 6.dp)

@Composable
fun Modifier.voidMarker(crStatus: CrStatus): Modifier {
    val voidMarkerHeight = with(LocalDensity.current) { 2.dp.toPx() }
    val voidStatus = stringResource(id = R.string.status_void)
    return conditional(crStatus == CrStatus.VOID) {
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

@Preview
@Composable
private fun RenderNew(@PreviewParameter(SampleProvider::class) transaction: Transaction2) {
    NewTransactionRenderer(DateTimeFormatter.ofLocalizedDate(FormatStyle.SHORT))
        .Render(transaction)
}

@Preview
@Composable
private fun RenderCompact(@PreviewParameter(SampleProvider::class) transaction: Transaction2) {
    CompactTransactionRenderer(
        DateTimeFormatInfo(DateTimeFormatter.ofPattern("EEE"), 4f),
        withOriginalAmount = true
    ).Render(transaction)
}

class SampleProvider : PreviewParameterProvider<Transaction2> {
    private val originalCurrency = CurrencyUnit("TRY", "₺", 2)
    override val values = sequenceOf(
        Transaction2(
            id = -1,
            _date = System.currentTimeMillis() / 1000,
            displayAmount = Money(CurrencyUnit.DebugInstance, 8000),
            originalAmount = Money(originalCurrency, 1234500),
            methodLabel = "CHEQUE",
            //methodIcon = "credit-card",
            referenceNumber = "1",
            accountId = -1,
            catId = 1,
            categoryPath = "Obst und Gemüse",
            comment = "Erika Musterfrau",
            //icon = "apple",
            year = 2022,
            month = 1,
            day = 1,
            week = 1,
            crStatus = CrStatus.VOID,
            tagList = listOf(
                Triple(1, "Hund", android.graphics.Color.RED),
                Triple(2, "Katz", android.graphics.Color.GREEN)
            ),
            accountType = 0
        ),
        Transaction2(
            id = -1,
            _date = System.currentTimeMillis() / 1000,
            displayAmount = Money(CurrencyUnit.DebugInstance, 7000),
            originalAmount = Money(originalCurrency, 2345600),
            accountId = -1,
            catId = SPLIT_CATID,
            party = DisplayParty(0, "Erika Musterfrau"),
            year = 2022,
            month = 1,
            day = 1,
            week = 1,
            tagList = listOf(
                Triple(1, "Hund", android.graphics.Color.RED),
                Triple(2, "Katz", android.graphics.Color.GREEN)
            ),
            accountType = 0
        )
    )
}