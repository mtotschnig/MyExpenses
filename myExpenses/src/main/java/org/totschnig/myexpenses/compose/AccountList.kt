package org.totschnig.myexpenses.compose

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.MenuEntry.Companion.delete
import org.totschnig.myexpenses.compose.MenuEntry.Companion.edit
import org.totschnig.myexpenses.compose.MenuEntry.Companion.toggle
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import timber.log.Timber
import kotlin.math.roundToInt

@Composable
fun AccountList(
    accountData: List<FullAccount>,
    grouping: AccountGrouping,
    selectedAccount: Long,
    onSelected: (Int) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (FullAccount) -> Unit,
    onHide: (Long) -> Unit,
    onToggleSealed: (FullAccount) -> Unit,
    expansionHandlerGroups: ExpansionHandler,
    expansionHandlerAccounts: ExpansionHandler
) {
    val context = LocalContext.current
    val collapsedGroupIds = expansionHandlerGroups.collapsedIds.collectAsState(initial = null).value
    val collapsedAccountIds =
        expansionHandlerAccounts.collapsedIds.collectAsState(initial = null).value

    if (collapsedGroupIds != null && collapsedAccountIds != null) {
        LazyColumn(
            modifier = Modifier.testTag(TEST_TAG_ACCOUNTS),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            var isGroupHidden by mutableStateOf(false)
            accountData.forEachIndexed { index, account ->
                getHeader(context, grouping, account, accountData.getOrNull(index - 1))?.let {
                    item {
                        Header(it.second) {
                            expansionHandlerGroups.toggle(it.first)
                        }
                    }
                    isGroupHidden = collapsedGroupIds.contains(it.first)
                }
                if (!isGroupHidden) {
                    item {
                        AccountCard(
                            account = account,
                            isCollapsed = collapsedAccountIds.contains(account.id.toString()),
                            isSelected = account.id == selectedAccount,
                            onSelected = { onSelected(index) },
                            onEdit = onEdit,
                            onDelete = onDelete,
                            onHide = onHide,
                            onToggleSealed = onToggleSealed,
                            toggleExpansion = { expansionHandlerAccounts.toggle(account.id.toString()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(header: String, onHeaderClick: () -> Unit) {
    Divider(color = colorResource(id = R.color.material_grey_300), thickness = 2.dp)
    Text(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onHeaderClick)
            .padding(
                horizontal = dimensionResource(id = R.dimen.drawer_padding),
                vertical = 4.dp
            ),
        text = header,
        style = MaterialTheme.typography.subtitle1,
        color = colorResource(id = R.color.material_grey)
    )
}

private fun getHeader(
    context: Context,
    grouping: AccountGrouping,
    account: FullAccount,
    previous: FullAccount?
): Pair<String, String>? {
    val needsHeader = previous == null ||
            when (grouping) {
                AccountGrouping.NONE -> account.id < 0 && previous.id > 0
                AccountGrouping.TYPE -> account.type != previous.type
                AccountGrouping.CURRENCY -> account.id == AggregateAccount.HOME_AGGREGATE_ID || account.currency != previous.currency
            }
    return if (needsHeader) {
        when (grouping) {
            AccountGrouping.NONE -> {
                val id = if (account.id > 0) "0" else "1"
                val title =
                    context.getString(if (account.id > 0) R.string.pref_manage_accounts_title else R.string.menu_aggregates)
                id to title
            }
            AccountGrouping.TYPE -> {
                val id = account.type?.ordinal ?: AccountType.values().size
                val title =
                    context.getString(account.type?.toStringResPlural() ?: R.string.menu_aggregates)
                id.toString() to title
            }
            AccountGrouping.CURRENCY -> {
                if (account.id == AggregateAccount.HOME_AGGREGATE_ID)
                    AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE to context.getString(R.string.menu_aggregates)
                else account.currency.code to Currency.create(account.currency.code, context)
                    .toString()
            }
        }
    } else null
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCard(
    account: FullAccount,
    isCollapsed: Boolean = false,
    isSelected: Boolean = false,
    onSelected: () -> Unit = {},
    onEdit: (Long) -> Unit = {},
    onDelete: (FullAccount) -> Unit = {},
    onHide: (Long) -> Unit = {},
    onToggleSealed: (FullAccount) -> Unit = { _ -> },
    toggleExpansion: () -> Unit = { }
) {
    val format = LocalCurrencyFormatter.current
    val showMenu = remember { mutableStateOf(false) }
    val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)

    Timber.i("Account %s isCollapsed: %b", account.label, isCollapsed)

    Column(
        modifier = Modifier
            .conditional(isSelected) {
                background(activatedBackgroundColor)
            }
            .combinedClickable(
                onClick = { onSelected() },
                onLongClick = { showMenu.value = true }
            )
            .padding(start = dimensionResource(id = R.dimen.drawer_padding))

    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val modifier = Modifier
                .padding(end = 6.dp)
                .size(dimensionResource(id = R.dimen.account_color_diameter_compose))
            val color = Color(account.color(LocalContext.current.resources))
            if (account.criterion == null) {
                ColorCircle(modifier, color) {
                    if (account.isAggregate) {
                        Text(fontSize = 18.sp, text = "Σ", color = Color.White)
                    }
                }
            } else {
                DonutInABox(
                    modifier = modifier,
                    progress = if (account.criterion > 0 == account.currentBalance > 0) {
                        (account.currentBalance * 100F / account.criterion).roundToInt()
                    } else 0,
                    fontSize = 10.sp,
                    strokeWidth = 10f,
                    color = color
                )
            }
            if (account.sealed) {
                Icon(
                    imageVector = Icons.Filled.Lock,
                    modifier = Modifier.padding(end = 4.dp),
                    contentDescription = stringResource(
                        id = R.string.content_description_closed
                    )
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.label)
                AnimatedVisibility(visible = isCollapsed) {
                    Text(
                        text = format.convAmount(account.currentBalance, account.currency)
                    )
                }

            }
            ExpansionHandle(isExpanded = !isCollapsed, toggle = toggleExpansion)
            val menu: Menu<FullAccount> = Menu(
                buildList {
                    if (account.id > 0) {
                        if (!account.sealed) {
                            add(edit { onEdit(it.id) })
                        }
                        add(delete { onDelete(it) })
                        add(MenuEntry(
                            icon = Icons.Filled.VisibilityOff,
                            label = R.string.menu_hide
                        ) {
                            onHide(it.id)
                        }
                        )
                        add(
                            toggle(account.sealed) {
                                onToggleSealed(it)
                            }
                        )
                    }
                }
            )
            HierarchicalMenu(showMenu, menu, account)
        }

        val visibleState = remember { MutableTransitionState(!isCollapsed) }
        visibleState.targetState = !isCollapsed
        AnimatedVisibility(visibleState) {
            Column(modifier = Modifier.padding(end = 16.dp)) {

                account.description?.let { Text(it) }
                SumRow(
                    R.string.opening_balance,
                    format.convAmount(account.openingBalance, account.currency)
                )
                SumRow(
                    R.string.sum_income,
                    format.convAmount(account.sumIncome, account.currency)
                )
                SumRow(
                    R.string.sum_expenses,
                    format.convAmount(account.sumExpense, account.currency)
                )

                if (account.sumTransfer != 0L) {
                    SumRow(
                        R.string.sum_transfer,
                        format.convAmount(account.sumTransfer, account.currency)
                    )
                }
                val borderColor = MaterialTheme.colors.onSurface
                SumRow(
                    R.string.current_balance,
                    format.convAmount(account.currentBalance, account.currency),
                    Modifier.drawBehind {
                        val strokeWidth = 2 * density
                        drawLine(
                            borderColor,
                            Offset(0f, 0f),
                            Offset(size.width, 0f),
                            strokeWidth
                        )
                    }
                )
                account.criterion?.let {
                    SumRow(
                        if (it > 0) R.string.saving_goal else R.string.credit_limit,
                        format.convAmount(it, account.currency)
                    )
                }

                account.total?.let {
                    SumRow(
                        R.string.menu_aggregates,
                        format.convAmount(it, account.currency)
                    )
                }
                if (!(account.isAggregate || account.type == AccountType.CASH)) {
                    SumRow(
                        R.string.total_cleared,
                        format.convAmount(account.clearedTotal, account.currency)
                    )
                    SumRow(
                        R.string.total_reconciled,
                        format.convAmount(account.reconciledTotal, account.currency)
                    )
                }
            }
        }
    }
}

@Composable
fun SumRow(label: Int, formattedAmount: String, modifier: Modifier = Modifier) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(label))
        Text(formattedAmount, modifier)
    }
}

@Preview
@Composable
fun AccountPreview() {
    AccountCard(
        account = FullAccount(
            id = 1,
            label = "Account",
            description = "Description",
            currency = CurrencyUnit.DebugInstance,
            _color = android.graphics.Color.RED,
            openingBalance = 0,
            currentBalance = 1000,
            sumIncome = 2000,
            sumExpense = 1000,
            sealed = true,
            type = AccountType.CASH,
            criterion = 5000
        )
    )
}