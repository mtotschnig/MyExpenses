package org.totschnig.myexpenses.next

import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import org.totschnig.myexpenses.designsystem.R as DesignR
import org.totschnig.myexpenses.next.core.ui.merchants.TransactionTile
import org.totschnig.myexpenses.designsystem.EmptyState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.FormatStyle
import kotlin.math.absoluteValue
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.SubMenuEntry
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.transactions.FutureCriterion
import org.totschnig.myexpenses.compose.transactions.SelectionHandler
import org.totschnig.myexpenses.compose.transactions.TransactionEvent
import org.totschnig.myexpenses.compose.transactions.TransactionEventHandler
import org.totschnig.myexpenses.compose.transactions.TransactionListContent
import org.totschnig.myexpenses.compose.transactions.transactionMenu
import org.totschnig.myexpenses.compose.transactions.voidMarker
import org.totschnig.myexpenses.next.transactions.TransactionEditActions
import org.totschnig.myexpenses.next.transactions.TransactionEditState
import org.totschnig.myexpenses.designsystem.BadgeStyle
import org.totschnig.myexpenses.designsystem.GroupItem
import org.totschnig.myexpenses.designsystem.GroupPosition
import org.totschnig.myexpenses.designsystem.Spacing
import org.totschnig.myexpenses.designsystem.TextBadge
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.model.sort.SortDirection
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.viewmodel.data.HeaderData
import org.totschnig.myexpenses.ui.DisplayParty
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import org.totschnig.myexpenses.next.core.ui.merchants.PayeeImage
import org.totschnig.myexpenses.next.core.common.NO_NAME
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.LabelIcons
import org.totschnig.myexpenses.next.core.ui.rememberDateFormatter

/**
 * Transaction list of the new design: transactions are grouped by day, each day has a header
 * with the date and the balance of the account at the end of that day.
 * Expects the account of [content] to be grouped by [Grouping.DAY], otherwise day headers are omitted.
 *
 * @param isReadOnly for lists other than of an account: transactions cannot be selected, tapping
 * them only opens the details, if there are [detailsContent]. The details have a menu only if
 * [TransactionListContent.modificationAllowed], without selecting and filtering, which belong to
 * the list of an account.
 * @param showDateAndAccount second line shows date and account instead of details, for lists
 * without day headers that span several accounts
 * @param detailsContent if given, tapping a transaction opens its details instead of its menu,
 * which then is found in the details. Adds content to the details, e.g. the contract of the transaction.
 * @param suggestedCategory path of the category suggested for a transaction without one, shown faded
 * @param categoryChip shown in the details below the title to change the category, null for
 * transactions whose category cannot be changed there
 * @param tagChips shown in the details below the category to set the tags, only in lists that
 * are not [isReadOnly]
 * @param isMarker whether a tag is a marker (one-off, reimbursed, business) rather than a group,
 * the list shows both kinds apart
 * @param editState what the details of the opened transaction can change, see [TransactionEditActions]
 * @param editActions change the opened transaction from its details, null in lists where it cannot
 * be changed
 */
@Composable
fun NextTransactionList(
    content: TransactionListContent,
    modifier: Modifier = Modifier,
    isReadOnly: Boolean = false,
    showDateAndAccount: Boolean = false,
    detailsContent: (@Composable (Transaction2) -> Unit)? = null,
    suggestedCategory: (transactionId: Long) -> String? = { null },
    categoryChip: (Transaction2) -> (@Composable (Modifier) -> Unit)? = { null },
    tagChips: (Transaction2) -> (@Composable (Modifier) -> Unit)? = { null },
    isMarker: (tagId: Long) -> Boolean = { false },
    payeeImage: (payeeId: Long) -> PayeeImage? = { null },
    /**
     * Lets the user choose the icon of a payee or load its logo from its web address, opened from
     * the details of [transaction], whose category helps to suggest icons
     */
    payeeImageDialog: (@Composable (payee: DisplayParty, transaction: Transaction2?, onDismiss: () -> Unit) -> Unit)? = null,
    editState: TransactionEditState? = null,
    editActions: TransactionEditActions? = null,
) {
    val lazyPagingItems = content.lazyPagingItems
    var openedId by rememberSaveable { mutableStateOf<Long?>(null) }
    if (detailsContent != null) {
        OpenedTransaction(
            content = content,
            openedId = openedId,
            onClose = {
                openedId = null
                editActions?.close()
            },
            isReadOnly = isReadOnly,
            categoryChip = categoryChip,
            tagChips = tagChips,
            payeeImage = payeeImage,
            payeeImageDialog = payeeImageDialog,
            editing = if (editState != null && editActions != null && !isReadOnly) editState to editActions else null,
            detailsContent = detailsContent
        )
    }

    if (lazyPagingItems.itemCount == 0) {
        if (lazyPagingItems.loadState.refresh !is LoadState.Loading) {
            NoTransactions(content.isFiltered, modifier)
        }
        return
    }

    val context = LocalContext.current
    val currencyFormatter = LocalCurrencyFormatter.current
    val futureCriterionDate = content.futureCriterion.startOfFuture()

    LazyColumn(
        modifier = modifier,
        // Keep the last transaction clear of the FAB
        contentPadding = PaddingValues(bottom = 88.dp)
    ) {
        transactionItems(lazyPagingItems, content.headerData as? HeaderData) { transaction, isFirst, isLast ->
            TransactionItem(
                transaction = transaction,
                isFirst = isFirst,
                isLast = isLast,
                isFuture = transaction.date >= futureCriterionDate,
                isReadOnly = isReadOnly,
                showDateAndAccount = showDateAndAccount,
                suggestedCategory = if (transaction.catId == null) suggestedCategory(transaction.id) else null,
                isMarker = isMarker,
                image = transaction.party?.id?.let(payeeImage),
                onOpen = if (detailsContent != null) {
                    { openedId = transaction.id }
                } else null,
                selectionHandler = content.selectionHandler,
                menu = {
                    transactionMenu(
                        content.modificationAllowed,
                        content.accountCount,
                        context,
                        currencyFormatter,
                        transaction,
                        content.onEvent
                    )
                },
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }
    }
}

/** The details of the transaction [openedId], if any, with the dialog for the image of its payee */
@Composable
private fun OpenedTransaction(
    content: TransactionListContent,
    openedId: Long?,
    onClose: () -> Unit,
    isReadOnly: Boolean,
    categoryChip: (Transaction2) -> (@Composable (Modifier) -> Unit)?,
    tagChips: (Transaction2) -> (@Composable (Modifier) -> Unit)?,
    payeeImage: (payeeId: Long) -> PayeeImage?,
    payeeImageDialog: (@Composable (payee: DisplayParty, transaction: Transaction2?, onDismiss: () -> Unit) -> Unit)?,
    editing: Pair<TransactionEditState, TransactionEditActions>?,
    detailsContent: @Composable (Transaction2) -> Unit,
) {
    // The last version found is kept while the list reloads, e.g. after editing the transaction
    // from its details. Not observed, it only bridges until the list has the transaction again.
    val lastOpened = remember { arrayOfNulls<Transaction2>(1) }
    val opened = openedId?.let { id ->
        content.lazyPagingItems.itemSnapshotList.find { it?.id == id } ?: lastOpened[0]?.takeIf { it.id == id }
    }
    lastOpened[0] = opened
    val context = LocalContext.current
    val currencyFormatter = LocalCurrencyFormatter.current
    var logoPayee by rememberSaveable { mutableStateOf<DisplayParty?>(null) }
    logoPayee?.let { payee -> payeeImageDialog?.invoke(payee, opened) { logoPayee = null } }
    val transaction = opened ?: return
    val canEdit = editing != null && content.modificationAllowed
    if (canEdit) LaunchedEffect(transaction.id) { editing.second.open(transaction.id) }
    TransactionDetailSheet(
        transaction = transaction,
        image = transaction.party?.id?.let(payeeImage),
        onImageClick = transaction.party?.takeIf { it.id != null && payeeImageDialog != null && !isReadOnly }
            ?.let { payee -> { logoPayee = payee } },
        menu = {
            transactionMenu(
                content.modificationAllowed,
                content.accountCount,
                context,
                currencyFormatter,
                transaction,
                object : TransactionEventHandler {
                    override fun invoke(event: TransactionEvent, transaction: Transaction2) {
                        // Nothing left to show
                        if (event == TransactionEvent.Delete) onClose()
                        content.onEvent(event, transaction)
                    }
                }
            ).let { if (isReadOnly) it.withoutListActions() else it }
        }.takeIf { !isReadOnly || content.modificationAllowed },
        onDismiss = onClose,
        categoryChip = categoryChip(transaction),
        tagChips = tagChips(transaction).takeIf { !isReadOnly },
        editing = if (canEdit) DetailsEditing(
            state = editing.first,
            actions = editing.second,
            onEditClassic = { content.onEvent(TransactionEvent.Edit, transaction) }
        ) else null
    ) {
        detailsContent(transaction)
    }
}

@Composable
private fun NoTransactions(isFiltered: Boolean, modifier: Modifier) {
    if (isFiltered) {
        Text(
            text = stringResource(R.string.next_no_matching_transactions),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = modifier
                .fillMaxSize()
                .wrapContentSize()
        )
    } else {
        // The first start: what makes the transactions useful later
        EmptyState(
            title = stringResource(R.string.next_no_transactions),
            hint = stringResource(R.string.next_no_transactions_hint),
            modifier = modifier
        )
    }
}

/** From when on transactions are shown as planned */
private fun FutureCriterion.startOfFuture(): ZonedDateTime = when (this) {
    FutureCriterion.Current -> ZonedDateTime.now(ZoneId.systemDefault())
    FutureCriterion.EndOfDay -> LocalDate.now().plusDays(1).atStartOfDay().atZone(ZoneId.systemDefault())
}

/**
 * The loaded transactions, with a header above each day if the account is grouped by day.
 * [itemContent] gets whether a transaction is the first and the last of its group.
 */
private fun LazyListScope.transactionItems(
    lazyPagingItems: LazyPagingItems<Transaction2>,
    headerData: HeaderData?,
    itemContent: @Composable (transaction: Transaction2, isFirst: Boolean, isLast: Boolean) -> Unit,
) {
    val withDayHeaders = headerData?.account?.grouping == Grouping.DAY
    val snapshot = lazyPagingItems.itemSnapshotList
    val firstLoadedIndex = snapshot.indexOfFirst { it != null }.coerceAtLeast(0)
    val lastLoadedIndex = snapshot.indexOfLast { it != null }

    // Paging requires a stable number of items to keep up with refreshes in the middle of the list,
    // so unloaded transactions and their headers before the loaded range get empty placeholders
    if (firstLoadedIndex > 0) {
        items(count = firstLoadedIndex, key = { "p_$it" }) {}
    }

    var lastHeaderId: Int? = null
    for (index in firstLoadedIndex..lastLoadedIndex) {
        val item = snapshot[index] ?: continue
        val headerId = item.headerId

        if (withDayHeaders && lastHeaderId == null) {
            val placeholderCount = headerData.groups.keys.count {
                if (headerData.account.sortDirection == SortDirection.DESC) it > headerId else it < headerId
            }
            repeat(placeholderCount) {
                item(key = "placeholder_header_$it") {}
            }
        }

        val isFirstInGroup = if (withDayHeaders) headerId != lastHeaderId else lastHeaderId == null
        if (withDayHeaders && isFirstInGroup) {
            item(key = "header_$headerId") {
                DayHeader(
                    date = item.date.toLocalDate(),
                    balance = headerData.groups[headerId]?.interimBalance
                )
            }
        }

        val nextItem = if (index + 1 < lazyPagingItems.itemCount) lazyPagingItems.peek(index + 1) else null
        val isLastInGroup = nextItem == null || (withDayHeaders && nextItem.headerId != headerId)

        item(key = "trans_${item.id}") {
            // Accessing the item through the index triggers loading of further pages
            lazyPagingItems[index]?.let { itemContent(it, isFirstInGroup, isLastInGroup) }
        }
        lastHeaderId = headerId
    }
}

@Composable
private fun DayHeader(
    date: LocalDate,
    balance: Money?,
) {
    val today = LocalDate.now()
    val formatter = rememberDateFormatter(FormatStyle.FULL)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = when (date) {
                today -> stringResource(R.string.next_today)
                today.minusDays(1) -> stringResource(R.string.next_yesterday)
                else -> date.format(formatter)
            },
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (balance != null) {
            Text(
                text = LocalCurrencyFormatter.current.convAmount(balance.amountMinor, balance.currencyUnit),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

/**
 * One transaction. Transactions of a day look like one card: only the outer corners are rounded,
 * and dividers separate the rows. Like in the classic list, a tap opens the menu of the transaction
 * and a long press starts the selection.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TransactionItem(
    transaction: Transaction2,
    isFirst: Boolean,
    isLast: Boolean,
    isFuture: Boolean,
    isReadOnly: Boolean,
    showDateAndAccount: Boolean,
    suggestedCategory: String?,
    isMarker: (tagId: Long) -> Boolean,
    image: PayeeImage?,
    /** Opens the details, if null, tapping opens the menu */
    onOpen: (() -> Unit)?,
    selectionHandler: SelectionHandler?,
    menu: () -> Menu,
    modifier: Modifier = Modifier,
) {
    val showMenu = rememberSaveable { mutableStateOf(false) }
    val isSelectable = selectionHandler?.isSelectable(transaction) == true
    val isSelected = selectionHandler?.isSelected(transaction) == true
    val isChoosing = isSelectable && (selectionHandler?.selectionCount ?: 0) > 0
    val chooseSeveralLabel = stringResource(DesignR.string.ds_choose_several)

    GroupItem(
        position = GroupPosition(isFirst, isLast),
        modifier = modifier,
        color = if (isSelected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceContainerLowest
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .conditional(isReadOnly && onOpen != null) {
                    clickable(onClick = onOpen!!)
                }
                .conditional(!isReadOnly) {
                    combinedClickable(
                        onLongClickLabel = chooseSeveralLabel.takeIf { isSelectable },
                        onLongClick = if (isSelectable) {
                            { selectionHandler.toggle(transaction) }
                        } else null,
                        onClick = {
                            if ((selectionHandler?.selectionCount ?: 0) == 0) {
                                if (onOpen != null) onOpen() else showMenu.value = true
                            } else if (isSelectable) {
                                selectionHandler.toggle(transaction)
                            }
                        }
                    )
                }
                // While choosing several, the state is not only shown by the color
                .conditional(isChoosing) { semantics { selected = isSelected } }
                .voidMarker(transaction.crStatus)
                .padding(horizontal = 16.dp, vertical = 12.dp)
                // Planned transactions in the future are shown faded
                .alpha(if (isFuture) 0.6f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Lists show the payee, the category is shown as text
            TransactionTile(transaction.party?.displayName, image, categoryIcon = null)
            Spacer(Modifier.width(16.dp))
            val (title, details, category) = transaction.itemTexts()
            val dateFormatter = rememberDateFormatter()
            val subtitle = if (showDateAndAccount) listOfNotNull(
                dateFormatter.format(transaction.date),
                transaction.accountLabel?.takeIf { it.isNotBlank() }
            ).joinToString(" · ") else details
            Column(Modifier.weight(1f)) {
                Text(
                    text = title ?: NO_NAME,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                // The category as badge, as in the list of contracts; a suggestion is outlined
                val badge = category?.let { it to BadgeStyle.Filled }
                    ?: suggestedCategory?.let { it.lastCategoryPart() to BadgeStyle.Suggestion }
                TransactionSubtitle(subtitle, badge, tagBadges(transaction, isMarker))
            }
            SignedAmount(
                money = transaction.displayAmount,
                modifier = Modifier.padding(start = 8.dp)
            )
            MenuActionSheet(showMenu, menu, title = title)
        }
    }
}

/** Groups and markers next to the category, each the first one and how many more */
private fun tagBadges(transaction: Transaction2, isMarker: (tagId: Long) -> Boolean): List<Pair<String, ImageVector>> {
    val (markers, groups) = transaction.tagList.partition { isMarker(it.id) }
    return listOf(groups to LabelIcons.group, markers to LabelIcons.marker)
        .mapNotNull { (tags, icon) ->
            tags.firstOrNull()?.let { (if (tags.size > 1) "${it.label} +${tags.size - 1}" else it.label) to icon }
        }
}

/** Second line of a transaction: details, category badge and tag badges, nothing if all are missing */
@Composable
private fun TransactionSubtitle(
    subtitle: String?,
    badge: Pair<String, BadgeStyle>?,
    tagBadges: List<Pair<String, ImageVector>>,
) {
    if (subtitle == null && badge == null && tagBadges.isEmpty()) return
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (subtitle != null) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false)
            )
        }
        if (badge != null) {
            TextBadge(
                badge.first,
                Modifier.padding(start = if (subtitle != null) Spacing.small else 0.dp),
                style = badge.second
            )
        }
        tagBadges.forEachIndexed { index, (text, icon) ->
            TextBadge(
                text,
                Modifier.padding(start = if (subtitle != null || badge != null || index > 0) Spacing.small else 0.dp),
                style = BadgeStyle.Outlined,
                icon = icon
            )
        }
    }
}

/**
 * Amount with explicit sign: incoming money green with "+", outgoing money red with "−"
 */
@Composable
internal fun SignedAmount(
    money: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val amount = money.amountMinor
    val colors = LocalColors.current
    val formatted = LocalCurrencyFormatter.current.convAmount(amount.absoluteValue, money.currencyUnit)
    Text(
        text = when {
            amount > 0 -> "+ $formatted"
            amount < 0 -> "− $formatted"
            else -> formatted
        },
        style = style,
        fontWeight = FontWeight.SemiBold,
        color = when {
            amount > 0 -> colors.income
            amount < 0 -> colors.expense
            else -> MaterialTheme.colorScheme.onSurface
        },
        maxLines = 1,
        modifier = modifier
    )
}

/**
 * Texts of a list item: the payee is the main information about a transaction. Without payee, the
 * counterpart account of a transfer or the category take its place.
 *
 * @param category shown as badge, the last part of the path, null if it is the title already
 */
private data class ItemTexts(val title: String?, val details: String?, val category: String?)

@Composable
private fun Transaction2.itemTexts(): ItemTexts {
    val transferLabel = transferAccountLabel?.let {
        if (displayAmount.amountMinor > 0) "← $it" else "→ $it"
    }
    val categoryLabel = if (isSplit) stringResource(org.totschnig.myexpenses.R.string.split_transaction)
    else categoryPath
    val commentLabel = comment?.takeIf { it.isNotBlank() }
    val title = party?.name ?: transferLabel ?: categoryLabel ?: commentLabel
    return ItemTexts(
        title = title,
        details = listOfNotNull(transferLabel, commentLabel).filter { it != title }.takeIf { it.isNotEmpty() }?.joinToString(" · "),
        category = categoryLabel?.takeIf { it != title }?.let { if (isSplit) it else it.lastCategoryPart() }
    )
}

/** "Supermarkt" of "Essen > Supermarkt", the full path is in the details */
private fun String.lastCategoryPart() = substringAfterLast('>').trim()

/**
 * Leaves out the actions that work on the list of an account: selecting, and the filter submenu
 */
private fun Menu.withoutListActions() = filterNot { it.command == "SELECT_TRANSACTION" || it is SubMenuEntry }

