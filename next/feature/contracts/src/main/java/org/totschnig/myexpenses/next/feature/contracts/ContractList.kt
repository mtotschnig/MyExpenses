package org.totschnig.myexpenses.next.feature.contracts

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.next.core.ui.LocalAmountFormatter
import org.totschnig.myexpenses.designsystem.BadgedTopBarAction
import org.totschnig.myexpenses.designsystem.EmptyState
import org.totschnig.myexpenses.designsystem.HighlightCard
import org.totschnig.myexpenses.designsystem.PageTopBar
import org.totschnig.myexpenses.next.core.model.ledger.CurrencyInfo
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractArea
import org.totschnig.myexpenses.next.data.contracts.ContractNews
import org.totschnig.myexpenses.next.data.contracts.ContractsOverview
import org.totschnig.myexpenses.next.data.contracts.CustomArea
import org.totschnig.myexpenses.next.data.contracts.Reserve
import org.totschnig.myexpenses.next.data.contracts.icon
import org.totschnig.myexpenses.next.data.contracts.labelRes

@Composable
internal fun ContractList(
    state: ContractsOverview,
    /** The category [state] is limited to, null for all contracts */
    area: ContractArea?,
    isIncome: Boolean,
    currency: CurrencyInfo,
    /** What the user can do with contracts, see [ContractActions] */
    actions: ContractActions,
    onOpen: (Contract) -> Unit,
    /** Unread news, counted on top of the list of all contracts, and as dot on their contracts */
    news: List<ContractNews>,
    /** Opens the news, null if there are none, not even read ones */
    onOpenNews: (() -> Unit)?,
    /** Opens the suggestions of [state] to decide on them */
    onOpenSuggestions: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val contractsWithNews = news.mapTo(HashSet()) { it.contractKey }
    // Contracts are only removed from the list of all contracts, where the user has the full picture
    var isEditing by rememberSaveable { mutableStateOf(false) }
    val canEdit = area == null
    LaunchedEffect(area) { if (!canEdit) isEditing = false }
    BackHandler(enabled = isEditing) { isEditing = false }
    var renaming by rememberSaveable { mutableStateOf<String?>(null) }
    val isArea = area != null

    renaming?.let { signature ->
        (state.active + state.ended + state.dismissed + state.suggestions + state.cancelled)
            .find { it.signature == signature }
            ?.let { RenameContractDialog(it, actions, onDismiss = { renaming = null }) }
    }

    // Custom categories can be managed even without contracts, so they get the toolbar anyway
    if (state.isEmpty && area !is CustomArea) {
        ContractsEmptyState(isArea, modifier, isIncome)
        return
    }

    Column(modifier.fillMaxSize()) {
        // News and suggestions are counted on icons of the toolbar, each opens a page of its own
        val openNews = onOpenNews.takeIf { area == null && !isEditing }
        val openSuggestions = onOpenSuggestions.takeIf { state.suggestions.isNotEmpty() && !isEditing }
        // Toolbar only if it has something to offer
        val hasToolbar = area is CustomArea || canEdit && !state.isEmpty || openNews != null || openSuggestions != null
        if (hasToolbar) {
            ListToolbar(
                area = area as? CustomArea,
                actions = actions,
                isEditing = isEditing,
                onToggleEditing = { isEditing = !isEditing }.takeIf { canEdit && !state.isEmpty },
                unreadNews = news.size,
                onOpenNews = openNews,
                suggestions = state.suggestions.size,
                onOpenSuggestions = openSuggestions
            )
        }

        // All contracts removed: nothing to show outside of the edit mode
        if (state.isEmpty || !isEditing && state.active.isEmpty() && state.ended.isEmpty() && state.suggestions.isEmpty() &&
            state.cancelled.isEmpty()
        ) {
            ContractsEmptyState(isArea, Modifier.weight(1f), isIncome)
            return@Column
        }

        val dismissLabel = stringResource(if (isIncome) R.string.next_income_dismiss else R.string.next_contracts_dismiss)
        val confirmLabel = stringResource(if (isIncome) R.string.next_income_confirm else R.string.next_contracts_confirm)
        val restoreLabel = stringResource(R.string.next_contracts_restore)
        val renameLabel = stringResource(R.string.next_contracts_rename)

        fun LazyListScope.section(contracts: List<Contract>, isDismissed: Boolean) {
            itemsIndexed(contracts, key = { _, contract -> contract.signature }) { index, contract ->
                ContractItem(
                    contract = contract,
                    currency = currency,
                    isFirst = index == 0,
                    isLast = index == contracts.lastIndex,
                    onClick = if (isEditing) {
                        { renaming = contract.signature }
                    } else {
                        { onOpen(contract) }
                    },
                    editAction = if (isEditing) {
                        if (isDismissed) EditAction(Icons.Default.Restore, restoreLabel) { actions.restore(contract) }
                        else EditAction(Icons.Default.RemoveCircleOutline, dismissLabel) { actions.dismiss(contract) }
                    } else null,
                    // Tapping the row renames it in edit mode, the action is announced as well
                    accessibilityActions = if (isEditing) listOf(
                        CustomAccessibilityAction(renameLabel) { renaming = contract.signature; true }
                    ) else emptyList(),
                    isFaded = isDismissed || !contract.isActive,
                    hasNews = contract.signature in contractsWithNews,
                    badge = when {
                        isIncome -> stringResource(R.string.next_salary)
                            .takeIf { state.isSalary(contract) }
                        contract.isReserve -> stringResource(R.string.next_reserve)
                        // Within the tab of a category, it goes without saying
                        area == null -> contract.area?.label()
                        else -> null
                    }
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = if (hasToolbar) 0.dp else 16.dp, bottom = 16.dp)
        ) {
            if (state.active.isNotEmpty()) {
                item(key = "summary") {
                    SummaryCard(
                        monthlyAmount = state.active.sumOf { it.monthlyAmount },
                        yearlyAmount = state.active.sumOf { it.yearlyAmount },
                        count = state.active.size,
                        currency = currency,
                        isIncome = isIncome,
                        reservesMonthlyAmount = state.active.filter { it.isReserve }.sumOf { it.monthlyAmount }
                    )
                }
            }
            state.active.groupBy { it.interval }.toSortedMap().forEach { (interval, group) ->
                item(key = "header_${interval.name}") {
                    SectionHeader(
                        title = stringResource(interval.labelRes),
                        count = group.size,
                        total = group.sumOf { it.lastAmount },
                        currency = currency
                    )
                }
                section(group, isDismissed = false)
            }
            if (state.cancelled.isNotEmpty()) {
                item(key = "header_cancelled") {
                    SectionHeader(
                        title = stringResource(if (isIncome) R.string.next_income_cancelled else R.string.next_contracts_cancelled),
                        count = state.cancelled.size,
                        total = null,
                        currency = currency
                    )
                }
                section(state.cancelled, isDismissed = false)
            }
            if (state.ended.isNotEmpty()) {
                item(key = "header_ended") {
                    SectionHeader(
                        title = stringResource(R.string.next_contracts_ended),
                        count = state.ended.size,
                        total = null,
                        currency = currency
                    )
                }
                section(state.ended, isDismissed = false)
            }
            if (isEditing && state.dismissed.isNotEmpty()) {
                item(key = "header_dismissed") {
                    SectionHeader(
                        title = stringResource(R.string.next_contracts_hidden),
                        count = state.dismissed.size,
                        total = null,
                        currency = currency
                    )
                }
                section(state.dismissed, isDismissed = true)
            }
        }
    }
}

/**
 * Actions above the list: the news and the suggestions with their number, editing an own
 * category and the edit mode
 *
 * @param area the own category shown, null for all contracts or a built-in category
 * @param onToggleEditing null if the list cannot be edited
 * @param onOpenNews null if there are no news, not even read ones
 * @param onOpenSuggestions null if there are no suggestions
 */
@Composable
private fun ListToolbar(
    area: CustomArea?,
    actions: ContractActions,
    isEditing: Boolean,
    onToggleEditing: (() -> Unit)?,
    unreadNews: Int,
    onOpenNews: (() -> Unit)?,
    suggestions: Int,
    onOpenSuggestions: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp),
        horizontalArrangement = Arrangement.End
    ) {
        if (onOpenNews != null) {
            val title = stringResource(R.string.next_news)
            BadgedTopBarAction(
                Icons.Outlined.Notifications,
                contentDescription = if (unreadNews == 0) title
                else "$title, ${pluralStringResource(R.plurals.next_news_unread_count, unreadNews, unreadNews)}",
                count = unreadNews,
                onClick = onOpenNews
            )
        }
        if (onOpenSuggestions != null) {
            BadgedTopBarAction(
                Icons.Outlined.Lightbulb,
                contentDescription = "${stringResource(R.string.next_contracts_suggestions)}, " +
                        pluralStringResource(R.plurals.next_contracts_suggestions_count, suggestions, suggestions),
                count = suggestions,
                onClick = onOpenSuggestions,
                isAlert = false
            )
        }
        if (area != null) {
            EditAreaButton(area, actions::renameArea, actions::deleteArea)
        }
        if (onToggleEditing != null) IconButton(onClick = onToggleEditing) {
            if (isEditing) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = stringResource(R.string.next_done),
                    tint = MaterialTheme.colorScheme.primary
                )
            } else {
                Icon(
                    Icons.AutoMirrored.Filled.List,
                    contentDescription = stringResource(R.string.next_contracts_edit),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ContractsEmptyState(isArea: Boolean, modifier: Modifier = Modifier, isIncome: Boolean = false) {
    EmptyState(
        title = stringResource(
            when {
                isIncome -> R.string.next_income_empty
                isArea -> R.string.next_contracts_empty_area
                else -> R.string.next_contracts_empty
            }
        ),
        hint = stringResource(
            when {
                isIncome -> R.string.next_income_empty_hint
                isArea -> R.string.next_contracts_empty_area_hint
                else -> R.string.next_contracts_empty_hint
            }
        ),
        modifier = modifier
    )
}

/**
 * Opens the editing of a category created by the user
 */
@Composable
private fun EditAreaButton(
    area: CustomArea,
    onRename: (CustomArea, String) -> Unit,
    onDelete: (CustomArea) -> Unit,
) {
    var showDialog by rememberSaveable { mutableStateOf(false) }
    IconButton(onClick = { showDialog = true }) {
        Icon(
            Icons.Default.Edit,
            contentDescription = stringResource(R.string.next_contracts_edit_area),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    if (showDialog) {
        EditAreaDialog(
            area = area,
            onRename = {
                onRename(area, it)
                showDialog = false
            },
            onDelete = {
                onDelete(area)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}

@Composable
private fun SummaryCard(
    monthlyAmount: Long,
    yearlyAmount: Long,
    count: Int,
    currency: CurrencyInfo,
    isIncome: Boolean,
    /** Part of [monthlyAmount] that is put aside, see [Reserve] */
    reservesMonthlyAmount: Long = 0,
) {
    val formatter = LocalAmountFormatter.current
    HighlightCard {
        Text(
            stringResource(if (isIncome) R.string.next_income_per_month else R.string.next_contracts_per_month),
            style = MaterialTheme.typography.labelLarge
        )
        Text(
            formatter.format(monthlyAmount, currency),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )
        if (reservesMonthlyAmount != 0L) {
            Text(
                stringResource(R.string.next_contracts_of_which_reserves, formatter.format(reservesMonthlyAmount, currency)),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        Text(
            stringResource(
                R.string.next_contracts_per_year,
                formatter.format(yearlyAmount, currency)
            ) + " · " + pluralStringResource(
                if (isIncome) R.plurals.next_income_count else R.plurals.next_contracts_count, count, count
            ),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    count: Int,
    total: Long?,
    currency: CurrencyInfo,
) {
    org.totschnig.myexpenses.designsystem.SectionHeader(
        title = title,
        count = count,
        trailing = total?.let { LocalAmountFormatter.current.format(it, currency) }
    )
}

internal class SuggestionActions(val confirm: EditAction, val reject: EditAction)

/**
 * The suggested contracts (or incomes) of a list on a page of their own, the user decides on them
 * with the buttons of each item. Goes back once all are decided.
 */
@Composable
internal fun ContractSuggestionsScreen(
    suggestions: List<Contract>,
    isIncome: Boolean,
    currency: CurrencyInfo,
    actions: ContractActions,
    onOpen: (Contract) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LaunchedEffect(suggestions.isEmpty()) { if (suggestions.isEmpty()) onBack() }
    val dismissLabel = stringResource(if (isIncome) R.string.next_income_dismiss else R.string.next_contracts_dismiss)
    val confirmLabel = stringResource(if (isIncome) R.string.next_income_confirm else R.string.next_contracts_confirm)
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        PageTopBar(onBack = onBack, title = stringResource(R.string.next_contracts_suggestions))
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp)
        ) {
            item(key = "hint") {
                Text(
                    stringResource(R.string.next_contracts_suggestions_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 4.dp, end = 4.dp, bottom = 12.dp)
                )
            }
            itemsIndexed(suggestions, key = { _, contract -> contract.signature }) { index, contract ->
                ContractItem(
                    contract = contract,
                    currency = currency,
                    isFirst = index == 0,
                    isLast = index == suggestions.lastIndex,
                    onClick = { onOpen(contract) },
                    editAction = null,
                    suggestionActions = SuggestionActions(
                        confirm = EditAction(Icons.Default.Check, confirmLabel) { actions.confirm(contract) },
                        reject = EditAction(Icons.Default.Close, dismissLabel) { actions.dismiss(contract) }
                    ),
                    accessibilityActions = emptyList(),
                    isFaded = false,
                    badge = when {
                        isIncome -> null
                        contract.isReserve -> stringResource(R.string.next_reserve)
                        else -> contract.area?.label()
                    }
                )
            }
        }
    }
}

internal class EditAction(
    val icon: ImageVector,
    val label: String,
    val onClick: () -> Unit,
)
