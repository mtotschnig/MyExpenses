package org.totschnig.myexpenses.compose

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
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
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.AGGREGATE_HOME_CURRENCY_CODE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
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
    onToggleExcludeFromTotals: (FullAccount) -> Unit,
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
                            onToggleExcludeFromTotals = onToggleExcludeFromTotals,
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
    Divider(color = colorResource(id = androidx.appcompat.R.color.material_grey_300), thickness = 2.dp)
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
                AccountGrouping.CURRENCY -> account.id == HOME_AGGREGATE_ID || account.currencyUnit != previous.currencyUnit
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
                if (account.id == HOME_AGGREGATE_ID)
                    AGGREGATE_HOME_CURRENCY_CODE to context.getString(R.string.menu_aggregates)
                else account.currency to Currency.create(account.currency, context)
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
    onToggleSealed: (FullAccount) -> Unit = {},
    onToggleExcludeFromTotals: (FullAccount) -> Unit = {},
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
            if (account.excludeFromTotals) {
                val contentColor = LocalContentColor.current
                Icon(
                    modifier = Modifier.drawBehind {
                        drawLine(contentColor, Offset(size.width/5,size.height/2), Offset(size.width/5*4, size.height/2), 5f)
                    },
                    imageVector = Icons.Filled.Functions,
                    contentDescription = stringResource(
                        id = R.string.menu_exclude_from_totals
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(text = account.label)
                AnimatedVisibility(visible = isCollapsed) {
                    Text(
                        text = format.convAmount(account.currentBalance, account.currencyUnit)
                    )
                }

            }
            ExpansionHandle(isExpanded = !isCollapsed, toggle = toggleExpansion)
            val menu = Menu(
                buildList {
                    if (account.id > 0) {
                        if (!account.sealed) {
                            add(edit("EDIT_ACCOUNT") { onEdit(account.id) })
                        }
                        add(delete("DELETE_ACCOUNT") { onDelete(account) })
                        add(MenuEntry(
                            icon = Icons.Filled.VisibilityOff,
                            label = R.string.menu_hide,
                            command = "HIDE_COMMAND"
                        ) {
                            onHide(account.id)
                        }
                        )
                        add(
                            toggle("ACCOUNT", account.sealed) {
                                onToggleSealed(account)
                            }
                        )
                        add(CheckableMenuEntry(
                            isChecked = account.excludeFromTotals,
                            label = R.string.menu_exclude_from_totals,
                            command = "EXCLUDE_FROM_TOTALS_COMMAND"
                        ) {
                            onToggleExcludeFromTotals(account)
                        })
                    }
                }
            )
            HierarchicalMenu(showMenu, menu)
        }

        val visibleState = remember { MutableTransitionState(!isCollapsed) }
        visibleState.targetState = !isCollapsed
        AnimatedVisibility(visibleState) {
            Column(modifier = Modifier.padding(end = 16.dp)) {

                account.description?.let { Text(it) }
                SumRow(
                    R.string.opening_balance,
                    format.convAmount(account.openingBalance, account.currencyUnit)
                )
                SumRow(
                    R.string.sum_income,
                    format.convAmount(account.sumIncome, account.currencyUnit)
                )
                SumRow(
                    R.string.sum_expenses,
                    format.convAmount(account.sumExpense, account.currencyUnit)
                )

                if (account.sumTransfer != 0L) {
                    SumRow(
                        R.string.sum_transfer,
                        format.convAmount(account.sumTransfer, account.currencyUnit)
                    )
                }
                val borderColor = MaterialTheme.colors.onSurface
                SumRow(
                    R.string.current_balance,
                    format.convAmount(account.currentBalance, account.currencyUnit),
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
                        format.convAmount(it, account.currencyUnit)
                    )
                }

                account.total?.let {
                    SumRow(
                        R.string.menu_aggregates,
                        format.convAmount(it, account.currencyUnit)
                    )
                }
                if (!(account.isAggregate || account.type == AccountType.CASH)) {
                    SumRow(
                        R.string.total_cleared,
                        format.convAmount(account.clearedTotal, account.currencyUnit)
                    )
                    SumRow(
                        R.string.total_reconciled,
                        format.convAmount(account.reconciledTotal, account.currencyUnit)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SumRow(label: Int, formattedAmount: String, modifier: Modifier = Modifier) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(stringResource(label), Modifier.weight(1f).basicMarquee(iterations = 1), maxLines = 1)
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
            currencyUnit = CurrencyUnit.DebugInstance,
            _color = android.graphics.Color.RED,
            openingBalance = 0,
            currentBalance = 1000,
            sumIncome = 2000,
            sumExpense = 1000,
            sealed = true,
            type = AccountType.CASH,
            criterion = 5000,
            excludeFromTotals = true
        )
    )
}