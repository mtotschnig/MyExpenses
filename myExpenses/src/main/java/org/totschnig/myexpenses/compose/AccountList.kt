package org.totschnig.myexpenses.compose

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Divider
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
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
import org.totschnig.myexpenses.model.AggregateAccount
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import kotlin.math.roundToInt

@Composable
fun AccountList(
    accountData: List<FullAccount>,
    grouping: AccountGrouping,
    selectedAccount: Long,
    onSelected: (Int) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onHide: (Long) -> Unit,
    onToggleSealed: (Long, Boolean) -> Unit,
    expansionHandlerGroups: ExpansionHandler,
    expansionHandlerAccounts: ExpansionHandler
) {
    val context = LocalContext.current
    val collapsedIds = expansionHandlerGroups.collapsedIds().value

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
                isGroupHidden = collapsedIds.contains(it.first)
            }
            if (!isGroupHidden) {
                item {
                    AccountCard(
                        account = account,
                        expansionHandler = expansionHandlerAccounts,
                        isSelected = account.id == selectedAccount,
                        onSelected = { onSelected(index) },
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onHide = onHide,
                        onToggleSealed = onToggleSealed
                    )
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
                val title = context.getString(if (account.id > 0) R.string.pref_manage_accounts_title else R.string.menu_aggregates)
                id to title
            }
            AccountGrouping.TYPE -> {
                val id = account.type?.ordinal ?: AccountType.values().size
                val title = context.getString(account.type?.toStringResPlural() ?: R.string.menu_aggregates)
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

@Composable
fun AccountCard(
    account: FullAccount,
    expansionHandler: ExpansionHandler,
    isSelected: Boolean = false,
    onSelected: () -> Unit = {},
    onEdit: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onHide: (Long) -> Unit = {},
    onToggleSealed: (Long, Boolean) -> Unit = { _, _ -> }
) {
    val format = LocalCurrencyFormatter.current
    val showMenu = remember { mutableStateOf(false) }
    val collapsedAccounts = expansionHandler.collapsedIds()

    val isCollapsed = remember {
        derivedStateOf {
            collapsedAccounts.value.contains(account.id.toString())
        }
    }.value

    Column(
        modifier = (if (isSelected)
            Modifier.background(colorResource(id = R.color.activatedBackground))
        else Modifier)
            .clickable {
                showMenu.value = true
            }
            .padding(start = dimensionResource(id = R.dimen.drawer_padding))

    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            val modifier = Modifier
                .padding(end = 6.dp)
                .size(dimensionResource(id = R.dimen.account_color_diameter_compose))
            val color = Color(account.color(LocalContext.current.resources))
            if (account.criterion == 0L) {
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
            ExpansionHandle(isExpanded = !isCollapsed) {
                expansionHandler.toggle(account.id.toString())
            }
            val menu: Menu<FullAccount> = Menu(
                buildList {
                    add(MenuEntry(
                        icon = Icons.Filled.List,
                        label = R.string.menu_show_transactions
                    ) {
                        onSelected()
                    })
                    if (account.id > 0) {
                        if (!account.sealed) {
                            add(edit { onEdit(it.id) })
                        }
                        add(delete { onDelete(it.id) })
                        add(MenuEntry(
                            icon = Icons.Filled.VisibilityOff,
                            label = R.string.menu_hide
                        ) {
                            onHide(it.id)
                        }
                        )
                        add(
                            toggle(account.sealed) {
                                onToggleSealed(it.id, !it.sealed)
                            }
                        )
                    }
                }
            )
            HierarchicalMenu(showMenu, menu, account)
        }

        AnimatedVisibility(visible = !isCollapsed) {
            Column(modifier = Modifier.padding(end = 16.dp)) {

                account.description?.let { Text(it) }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.opening_balance))
                    Text(
                        text = format.convAmount(account.openingBalance, account.currency)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.sum_income))
                    Text(
                        text = format.convAmount(account.sumIncome, account.currency)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.sum_expenses))
                    Text(
                        text = format.convAmount(account.sumExpense, account.currency)
                    )
                }
                if (account.sumTransfer != 0L) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.sum_transfer))
                        Text(
                            text = format.convAmount(account.sumTransfer, account.currency)
                        )
                    }
                }
                val borderColor = MaterialTheme.colors.onSurface
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(id = R.string.current_balance))
                    Text(
                        modifier = Modifier.drawBehind {
                            val strokeWidth = 2 * density
                            drawLine(
                                borderColor,
                                Offset(0f, 0f),
                                Offset(size.width, 0f),
                                strokeWidth
                            )
                        },
                        text = format.convAmount(account.currentBalance, account.currency)
                    )
                }
            }
        }
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
        ),
        expansionHandler = object: ExpansionHandler {

            @Composable
            override fun collapsedIds(): State<Set<String>>  = remember { mutableStateOf(emptySet()) }

            override fun toggle(id: String) {

            }

        }
    )
}