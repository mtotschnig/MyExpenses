package org.totschnig.myexpenses.compose

import android.content.Context
import android.graphics.Color
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import dev.burnoo.compose.rememberpreference.rememberBooleanPreference
import dev.burnoo.compose.rememberpreference.rememberStringSetPreference
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

const val EXPANSION_PREF_PREFIX = "ACCOUNT_EXPANSION_"

@Composable
fun AccountList(
    accountData: List<FullAccount>,
    grouping: AccountGrouping,
    selectedAccount: Long,
    onSelected: (Int) -> Unit,
    onEdit: (Long) -> Unit,
    onDelete: (Long) -> Unit,
    onHide: (Long) -> Unit,
    onToggleSealed: (Long, Boolean) -> Unit
) {
    val context = LocalContext.current
    val collapsedHeaders = rememberStringSetPreference(
        keyName = "collapsedHeadersDrawer_" + grouping.name,
        initialValue = null,
        defaultValue = emptySet()
    )
    collapsedHeaders.value?.let { set ->

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            var isGroupHidden = false
            accountData.forEachIndexed { index, account ->
                getHeader(context, grouping, account, accountData.getOrNull(index - 1))?.let {
                    item {
                        Header(it.second) {
                            if (set.contains(it.first)) {
                                collapsedHeaders.value = set - it.first
                            } else {
                                collapsedHeaders.value = set + it.first
                            }
                        }
                    }
                    isGroupHidden = collapsedHeaders.value?.contains(it.first) ?: true
                }
                if (!isGroupHidden) {
                    item {
                        //TODO migrate from legacy preferences
                        val isExpanded = rememberBooleanPreference(
                            keyName = EXPANSION_PREF_PREFIX + when {
                                account.id > 0 -> account.id
                                account.id == AggregateAccount.HOME_AGGREGATE_ID -> AggregateAccount.AGGREGATE_HOME_CURRENCY_CODE
                                else -> account.currency.code
                            },
                            initialValue = null,
                            defaultValue = true
                        )
                        AccountCard(
                            account = account,
                            isExpanded = isExpanded,
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
                val id =
                    if (account.id > 0) R.string.pref_manage_accounts_title else R.string.menu_aggregates
                id.toString() to context.getString(id)
            }
            AccountGrouping.TYPE -> {
                val id = account.type?.toStringResPlural() ?: R.string.menu_aggregates
                id.toString() to context.getString(id)
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
    isExpanded: MutableState<Boolean?>,
    isSelected: Boolean = false,
    onSelected: () -> Unit = {},
    onEdit: (Long) -> Unit = {},
    onDelete: (Long) -> Unit = {},
    onHide: (Long) -> Unit = {},
    onToggleSealed: (Long, Boolean) -> Unit = { _, _ -> }
) {
    val format = LocalCurrencyFormatter.current
    val showMenu = remember { mutableStateOf(false) }

    isExpanded.value?.let {
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
                ColorCircle(
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(dimensionResource(id = R.dimen.account_color_diameter_compose)),
                    color = account.color
                )
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
                    AnimatedVisibility(visible = !it) {
                        Text(
                            text = format.convAmount(account.currentBalance, account.currency)
                        )
                    }

                }
                ExpansionHandle(isExpanded = it) {
                    isExpanded.value = !it
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

            AnimatedVisibility(visible = it) {
                Column(modifier = Modifier.padding(end = 16.dp)) {

                    Text(account.description)
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
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.sum_transfer))
                        Text(
                            text = format.convAmount(account.sumTransfer, account.currency)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(id = R.string.current_balance))
                        Text(
                            text = format.convAmount(account.currentBalance, account.currency)
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun AccountPreview() {
    val isExpanded = remember {
        mutableStateOf<Boolean?>(true)
    }
    AccountCard(
        account = FullAccount(
            id = 1,
            label = "Account",
            description = "Description",
            currency = CurrencyUnit.DebugInstance,
            color = Color.RED,
            openingBalance = 0,
            currentBalance = 1000,
            sumIncome = 2000,
            sumExpense = 1000,
            sealed = true,
            type = AccountType.CASH
        ),
        isExpanded = isExpanded
    )
}