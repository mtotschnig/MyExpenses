package org.totschnig.myexpenses.next

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import java.text.NumberFormat
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.designsystem.Shapes
import org.totschnig.myexpenses.designsystem.theme.LocalSemanticColors
import org.totschnig.myexpenses.designsystem.ActionCard
import org.totschnig.myexpenses.designsystem.GroupItem
import org.totschnig.myexpenses.designsystem.GroupPosition
import org.totschnig.myexpenses.designsystem.GroupTitle
import org.totschnig.myexpenses.designsystem.InviteCard
import org.totschnig.myexpenses.designsystem.MenuButton
import org.totschnig.myexpenses.designsystem.MenuEntry
import org.totschnig.myexpenses.designsystem.OverviewCard
import sh.calvin.reorderable.ReorderableCollectionItemScope
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import org.totschnig.myexpenses.next.core.ui.icons.CatalogIcon
import androidx.compose.ui.semantics.contentDescription
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.NextPreviewTheme

/**
 * UI models of the overview screen. They are deliberately independent of the data layer;
 * mapping from the view models happens in the caller.
 */
data class OverviewBudget(
    val key: String,
    val title: String,
    /** Spent share of the budget, 1f = fully spent, values above 1f = exceeded, null while loading */
    val progress: Float?,
    val icon: ImageVector = Icons.Default.Savings,
    /** Name of an icon of the [org.totschnig.myexpenses.next.icons.IconCatalog] shown in place of [icon] */
    val catalogIcon: String? = null,
)

/** The budgets on the overview and what can be done with them, from the feature that has them */
class OverviewBudgets(
    /** null while loading */
    val budgets: List<OverviewBudget>?,
    /** Below the budgets, e.g. what is still free in the current period */
    val summary: String? = null,
    /** Without budgets, whether the user closed the invitation to a first one */
    val inviteDismissed: Boolean = false,
    val onDismissInvite: () -> Unit = {},
    val onClick: (OverviewBudget) -> Unit = {},
    val onShowAll: () -> Unit = {},
    val onAdd: () -> Unit = {},
)

data class OverviewAccount(
    val id: Long,
    val label: String,
    /** Already formatted balance, e.g. "3.205,99 €". */
    val balance: String,
    val isNegative: Boolean,
    val color: Color,
    val bankId: Long? = null,
)

data class OverviewSection(
    val title: String,
    val accounts: List<OverviewAccount>,
)

@Composable
fun NextOverviewScreen(
    sections: List<OverviewSection>,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    /** null without the feature of budgets */
    budgets: OverviewBudgets? = null,
    onAccountClick: (OverviewAccount) -> Unit = {},
    onAddAccount: () -> Unit = {},
    /** Called with the ids of all shown accounts in their new order, after the user moved one */
    onReorderAccounts: (List<Long>) -> Unit = {},
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
    /** First card, above the budgets, e.g. the balance of the current month */
    header: (@Composable (Modifier) -> Unit)? = null,
) {
    var isReordering by rememberSaveable { mutableStateOf(false) }
    BackHandler(enabled = isReordering) { isReordering = false }

    // Local copy, so that moves are shown immediately. It is replaced by fresh data,
    // which after persisting a move comes back in the same order.
    var orderedSections by remember(sections) { mutableStateOf(sections) }

    fun move(fromId: Long, toId: Long): Boolean {
        orderedSections = moveAccount(orderedSections, fromId, toId) ?: return false
        return true
    }

    fun persistOrder() {
        onReorderAccounts(orderedSections.flatMap { section -> section.accounts.map { it.id } })
    }

    val haptic = LocalHapticFeedback.current
    val canReorder = orderedSections.sumOf { it.accounts.size } > 1
    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        val fromId = from.key as? Long ?: return@rememberReorderableLazyListState
        val toId = to.key as? Long ?: return@rememberReorderableLazyListState
        if (move(fromId, toId)) {
            haptic.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
            // The background reaches behind the status bar, the content stays below it
            .windowInsetsPadding(WindowInsets.statusBars)
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier.weight(1f),
            contentPadding = contentPadding
        ) {
            if (header != null) {
                item(key = "header") {
                    header(Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp))
                }
            }
            if (budgets != null) budgetItem(budgets)
            if (orderedSections.isEmpty()) {
                item(key = "no_accounts") {
                    ActionCard(
                        icon = Icons.Default.AccountBalance,
                        title = stringResource(R.string.next_no_accounts),
                        text = stringResource(R.string.next_no_accounts_hint),
                        confirmLabel = stringResource(R.string.next_add_account),
                        onConfirm = onAddAccount,
                        confirmIcon = Icons.Default.Add,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 24.dp)
                    )
                }
            }
            orderedSections.forEachIndexed { sectionIndex, section ->
                item(key = "header_${section.title}") {
                    // Accounts are added and sorted rarely, so instead of dedicated rows these actions
                    // only take up the trailing end of the first section header
                    GroupTitle(
                        title = section.title,
                        action = if (sectionIndex == 0) {
                            {
                                AccountsActions(
                                    isReordering = isReordering,
                                    onReorderingChange = { isReordering = it },
                                    canReorder = canReorder,
                                    onAddAccount = onAddAccount,
                                    // Once the invitation is dismissed, budgets are added from here
                                    onAddBudget = budgets?.takeIf { it.budgets?.isEmpty() == true && it.inviteDismissed }
                                        ?.onAdd
                                )
                            }
                        } else null
                    )
                }
                itemsIndexed(section.accounts, key = { _, account -> account.id }) { index, account ->
                    ReorderableItem(reorderableState, key = account.id, enabled = isReordering) { isDragging ->
                        AccountItem(
                            account = account,
                            isFirst = index == 0,
                            isLast = index == section.accounts.lastIndex,
                            isDragging = isDragging,
                            bankIcon = bankIcon,
                            onClick = { onAccountClick(account) }.takeIf { !isReordering },
                            dragHandle = if (isReordering) {
                                { DragHandle(onDragStopped = ::persistOrder) }
                            } else null,
                            accessibilityActions = if (isReordering) moveActions(
                                previous = section.accounts.getOrNull(index - 1),
                                next = section.accounts.getOrNull(index + 1),
                                onMove = { target -> move(account.id, target.id).also { if (it) persistOrder() } }
                            ) else emptyList(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
            item { Spacer(Modifier.padding(bottom = 16.dp)) }
        }
    }
}

/**
 * [sections] with the account [fromId] moved to the place of [toId], or null when they are not in
 * the same section. Accounts can only be moved within their section, since the section follows
 * from their type.
 */
private fun moveAccount(sections: List<OverviewSection>, fromId: Long, toId: Long): List<OverviewSection>? {
    val sectionIndex = sections.indexOfFirst { section -> section.accounts.any { it.id == fromId } }
    val section = sections.getOrNull(sectionIndex) ?: return null
    val fromIndex = section.accounts.indexOfFirst { it.id == fromId }
    val toIndex = section.accounts.indexOfFirst { it.id == toId }
    if (toIndex == -1) return null
    return sections.toMutableList().apply {
        set(sectionIndex, section.copy(accounts = section.accounts.toMutableList().apply {
            add(toIndex, removeAt(fromIndex))
        }))
    }
}

private fun LazyListScope.budgetItem(budgets: OverviewBudgets) {
    // Nothing while loading, so that an invitation does not flash before the budgets
    val shownBudgets = budgets.budgets ?: return
    if (shownBudgets.isNotEmpty()) item(key = "budgets") {
        BudgetCard(
            budgets = shownBudgets,
            onBudgetClick = budgets.onClick,
            onShowAll = budgets.onShowAll,
            onAdd = budgets.onAdd,
            summary = budgets.summary,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    } else if (!budgets.inviteDismissed) item(key = "budget_invite") {
        InviteCard(
            icon = Icons.Default.Savings,
            title = stringResource(R.string.next_add_budget),
            text = stringResource(R.string.next_budget_invite_text),
            onClick = budgets.onAdd,
            onDismiss = budgets.onDismissInvite,
            dismissLabel = stringResource(R.string.next_budget_invite_dismiss),
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

/** At the end of the first section header: done while reordering, otherwise a menu */
@Composable
private fun AccountsActions(
    isReordering: Boolean,
    onReorderingChange: (Boolean) -> Unit,
    canReorder: Boolean,
    onAddAccount: () -> Unit,
    onAddBudget: (() -> Unit)?,
) {
    if (isReordering) {
        IconButton(onClick = { onReorderingChange(false) }) {
            Icon(
                Icons.Default.Check,
                contentDescription = stringResource(R.string.next_done),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    } else {
        val addAccountLabel = stringResource(R.string.next_add_account)
        val reorderLabel = stringResource(R.string.next_reorder_accounts)
        val addBudgetLabel = stringResource(R.string.next_add_budget)
        MenuButton(
            entries = listOfNotNull(
                MenuEntry(addAccountLabel, Icons.Default.Add, onAddAccount),
                MenuEntry(reorderLabel, Icons.Default.SwapVert) { onReorderingChange(true) }
                    .takeIf { canReorder },
                onAddBudget?.let { MenuEntry(addBudgetLabel, Icons.Default.Savings, it) }
            ),
            contentDescription = stringResource(R.string.next_accounts_more)
        )
    }
}

@Composable
private fun ReorderableCollectionItemScope.DragHandle(onDragStopped: () -> Unit) {
    val haptic = LocalHapticFeedback.current
    Icon(
        Icons.Default.DragHandle,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .draggableHandle(
                onDragStarted = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
                },
                onDragStopped = {
                    haptic.performHapticFeedback(HapticFeedbackType.GestureEnd)
                    onDragStopped()
                }
            )
            .padding(12.dp)
    )
}

/** Dragging is not accessible, so screen reader users move accounts step by step */
@Composable
private fun moveActions(
    previous: OverviewAccount?,
    next: OverviewAccount?,
    onMove: (OverviewAccount) -> Boolean,
): List<CustomAccessibilityAction> {
    val moveUp = stringResource(org.totschnig.myexpenses.R.string.action_move_up)
    val moveDown = stringResource(org.totschnig.myexpenses.R.string.action_move_down)
    return listOfNotNull(
        previous?.let { CustomAccessibilityAction(moveUp) { onMove(it) } },
        next?.let { CustomAccessibilityAction(moveDown) { onMove(it) } },
    )
}

@Composable
private fun BudgetCard(
    budgets: List<OverviewBudget>?,
    onBudgetClick: (OverviewBudget) -> Unit,
    onShowAll: () -> Unit,
    onAdd: () -> Unit,
    summary: String?,
    modifier: Modifier = Modifier,
) {
    OverviewCard(
        title = stringResource(R.string.next_budgets),
        modifier = modifier,
        actionLabel = stringResource(R.string.next_show_all),
        onAction = onShowAll,
        edgeToEdge = true
    ) {
        LazyRow(
            // As high as the add button, also while loading
            modifier = Modifier.heightIn(min = BudgetRingSize),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (budgets == null) return@LazyRow
            items(budgets, key = { it.key }) { budget ->
                BudgetRing(budget, onClick = { onBudgetClick(budget) })
            }
            item(key = "add") {
                BudgetTile(label = null, onClick = onAdd) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = stringResource(R.string.next_add_budget),
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }
        if (summary != null) {
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp)
            )
        }
    }
}

private val BudgetTileWidth = 64.dp
private val BudgetRingSize = 56.dp

/**
 * A circle of [BudgetRingSize] with an optional short [label] below, shared by the budget rings and the add button
 */
@Composable
private fun BudgetTile(
    label: String?,
    onClick: () -> Unit,
    labelColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    circle: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .width(BudgetTileWidth)
            .clip(Shapes.chip)
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(BudgetRingSize)
                .clip(CircleShape)
        ) {
            circle()
        }
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = labelColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun BudgetRing(
    budget: OverviewBudget,
    onClick: () -> Unit,
) {
    val colors = LocalSemanticColors.current
    val progress = budget.progress
    val ringColor = when {
        progress == null -> Color.Transparent
        progress > 1f -> colors.expense
        progress > 0.75f -> LocalSemanticColors.current.warning
        else -> colors.income
    }
    val trackColor = MaterialTheme.colorScheme.surfaceContainerHigh
    val percentFormat = remember { NumberFormat.getPercentInstance() }
    BudgetTile(
        label = budget.title,
        labelColor = if (progress != null && progress > 1f) colors.expense else MaterialTheme.colorScheme.onSurfaceVariant,
        onClick = onClick
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 4.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                drawArc(trackColor, 0f, 360f, false, topLeft, arcSize, style = Stroke(stroke))
                drawArc(
                    ringColor, -90f, 360f * (progress ?: 0f).coerceIn(0f, 1f), false, topLeft, arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round)
                )
            }
            val description = progress?.let { percentFormat.format(it) }
            if (budget.catalogIcon != null) {
                CatalogIcon(
                    budget.catalogIcon,
                    Modifier.semantics { description?.let { contentDescription = it } }
                )
            } else Icon(
                budget.icon,
                contentDescription = description,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AccountItem(
    account: OverviewAccount,
    isFirst: Boolean,
    isLast: Boolean,
    isDragging: Boolean,
    bankIcon: (@Composable (Modifier, Long) -> Unit)?,
    onClick: (() -> Unit)?,
    dragHandle: (@Composable () -> Unit)?,
    accessibilityActions: List<CustomAccessibilityAction>,
    modifier: Modifier = Modifier,
) {
    // Each account is a separate list item so that it can be dragged. Together they look like
    // one card: only the outer corners are rounded, and dividers separate the rows.
    GroupItem(
        position = GroupPosition(isFirst, isLast),
        modifier = modifier.semantics { customActions = accessibilityActions },
        isLifted = isDragging
    ) {
        AccountRow(account, bankIcon, onClick, dragHandle)
    }
}

@Composable
private fun AccountRow(
    account: OverviewAccount,
    bankIcon: (@Composable (Modifier, Long) -> Unit)?,
    onClick: (() -> Unit)?,
    dragHandle: (@Composable () -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            // The drag handle brings its own padding for a large enough touch target
            .padding(start = 16.dp, end = if (dragHandle != null) 0.dp else 8.dp)
            .padding(vertical = if (dragHandle != null) 12.dp else 20.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val iconModifier = Modifier.size(32.dp)
        if (account.bankId != null && bankIcon != null) {
            bankIcon(iconModifier, account.bankId)
        } else {
            Box(
                modifier = iconModifier
                    .clip(CircleShape)
                    .background(account.color),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = account.label.take(1).uppercase(),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        Text(
            text = account.label,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = account.balance,
            style = MaterialTheme.typography.bodyLarge,
            color = if (account.isNegative) MaterialTheme.colorScheme.onSurface else LocalSemanticColors.current.income,
            modifier = Modifier.padding(start = 8.dp)
        )
        if (dragHandle != null) {
            dragHandle()
        } else {
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private val previewBudgets = listOf(
    OverviewBudget("1", "Lebensmittel", 0.8f),
    OverviewBudget("2", "Freizeit", 0.3f),
    OverviewBudget("3", "Urlaub", 0.55f),
    OverviewBudget("4", "Haushalt", 1.1f),
)

private val previewSections = listOf(
    OverviewSection(
        "Tägliche Konten", listOf(
            OverviewAccount(1, "Girokonto", "3.205,99 €", false, Color(0xFFE53935)),
            OverviewAccount(2, "Kreditkarte", "-256,36 €", true, Color(0xFF1E88E5)),
            OverviewAccount(3, "PayPal", "32,11 €", false, Color(0xFF283593)),
        )
    ),
    OverviewSection(
        "Investments", listOf(
            OverviewAccount(4, "Direkt Depot", "12.187,22 €", false, Color(0xFFFB8C00)),
        )
    ),
)

@Preview(name = "Light", heightDp = 640)
@Preview(name = "Dark", heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun NextOverviewScreenPreview() {
    NextPreviewTheme {
        NextOverviewScreen(sections = previewSections, budgets = OverviewBudgets(previewBudgets))
    }
}

@Preview(name = "No accounts", heightDp = 640)
@Composable
private fun NextOverviewScreenEmptyPreview() {
    NextPreviewTheme {
        NextOverviewScreen(sections = emptyList(), budgets = OverviewBudgets(previewBudgets))
    }
}
