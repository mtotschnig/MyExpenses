package org.totschnig.myexpenses.compose

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.MenuEntry.Companion.delete
import org.totschnig.myexpenses.compose.MenuEntry.Companion.edit
import org.totschnig.myexpenses.compose.MenuEntry.Companion.toggle
import org.totschnig.myexpenses.compose.scrollbar.LazyColumnWithScrollbarAndBottomPadding
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.DEFAULT_FLAG_ID
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.AGGREGATE_HOME_CURRENCY_CODE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.isolateText
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import java.text.DecimalFormat

@Composable
fun AccountList(
    accountData: List<FullAccount>,
    grouping: AccountGrouping,
    selectedAccount: Long,
    listState: LazyListState,
    showEquivalentWorth: Boolean,
    onSelected: (Long) -> Unit,
    onEdit: (FullAccount) -> Unit,
    onDelete: (FullAccount) -> Unit,
    onSetFlag: (Long, Long?) -> Unit,
    onToggleSealed: (FullAccount) -> Unit,
    onToggleExcludeFromTotals: (FullAccount) -> Unit,
    onToggleDynamicExchangeRate: ((FullAccount) -> Unit)?,
    expansionHandlerGroups: ExpansionHandler,
    expansionHandlerAccounts: ExpansionHandler,
    bankIcon: (@Composable (Modifier, Long) -> Unit)?,
    flags: List<AccountFlag>
) {
    val context = LocalContext.current
    val collapsedGroupIds = expansionHandlerGroups.state.collectAsState(initial = null).value
    val expandedAccountIds =
        expansionHandlerAccounts.state.collectAsState(initial = null).value

    if (collapsedGroupIds != null && expandedAccountIds != null) {
        val grouped: Map<String, List<FullAccount>> =
            accountData.groupBy { getHeaderId(grouping, it) }
        LazyColumnWithScrollbarAndBottomPadding(
            modifier = Modifier.background(MaterialTheme.colorScheme.background),
            state = listState,
            itemsAvailable = accountData.size + grouped.size,
            withFab = false,
            testTag = TEST_TAG_ACCOUNTS,
        ) {
            grouped.forEach { group ->
                val headerId = group.key
                val isGroupHidden = collapsedGroupIds.contains(headerId)
                item {
                    Header(getHeaderTitle(context, grouping, group.value.first()), !isGroupHidden) {
                        expansionHandlerGroups.toggle(headerId)
                    }
                }
                group.value.forEachIndexed { index, account ->
                    if (!isGroupHidden) {
                        item(key = account.id) {
                            //TODO add collectionItemInfo
                            AccountCard(
                                account = account,
                                isCollapsed = !expandedAccountIds.contains(account.id.toString()),
                                isSelected = account.id == selectedAccount,
                                onSelected = { onSelected(account.id) },
                                onEdit = onEdit,
                                onDelete = onDelete,
                                onSetFlag = onSetFlag,
                                onToggleSealed = onToggleSealed,
                                onToggleExcludeFromTotals = onToggleExcludeFromTotals,
                                onToggleDynamicExchangeRate = onToggleDynamicExchangeRate,
                                toggleExpansion = { expansionHandlerAccounts.toggle(account.id.toString()) },
                                bankIcon = bankIcon,
                                showEquivalentWorth = showEquivalentWorth,
                                flags = flags
                            )
                            if (index != group.value.lastIndex) {
                                Spacer(Modifier.height(10.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Header(
    header: String,
    isExpanded: Boolean,
    onHeaderClick: () -> Unit,
) {
    HorizontalDivider(
        color = colorResource(id = androidx.appcompat.R.color.material_grey_300),
        thickness = 2.dp
    )
    Row(
        modifier = Modifier
            .clickable(onClick = onHeaderClick)
            .semantics(mergeDescendants = true) {}
            .padding(start = dimensionResource(id = R.dimen.drawer_padding)),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = header,
            style = MaterialTheme.typography.titleMedium,
            color = colorResource(id = R.color.material_grey)
        )
        val rotationAngle by animateFloatAsState(
            targetValue = if (isExpanded) 0F else 180F
        )
        Icon(
            modifier = Modifier
                .minimumInteractiveComponentSize()
                .rotate(rotationAngle),
            imageVector = Icons.Default.ExpandLess,
            contentDescription = stringResource(
                id = if (isExpanded) R.string.collapse
                else R.string.expand
            )
        )
    }
}

private fun getHeaderId(
    grouping: AccountGrouping,
    account: FullAccount,
) = when (grouping) {
    AccountGrouping.NONE -> if (account.id > 0) "0" else "1"

    AccountGrouping.TYPE -> account.type.name

    AccountGrouping.CURRENCY ->
        if (account.id == HOME_AGGREGATE_ID) AGGREGATE_HOME_CURRENCY_CODE else account.currency
}

private fun getHeaderTitle(
    context: Context,
    grouping: AccountGrouping,
    account: FullAccount,
) = when (grouping) {
    AccountGrouping.NONE -> context.getString(
        if (account.id > 0) R.string.pref_manage_accounts_title else R.string.menu_aggregates
    )

    AccountGrouping.TYPE ->
        if (account.isAggregate) context.getString(R.string.menu_aggregates) else
            account.type.localizedName(context)

    AccountGrouping.CURRENCY -> if (account.id == HOME_AGGREGATE_ID)
        context.getString(R.string.menu_aggregates)
    else
        Currency.create(account.currency, context).toString()
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AccountCard(
    account: FullAccount,
    isCollapsed: Boolean = false,
    isSelected: Boolean = false,
    showEquivalentWorth: Boolean = false,
    onSelected: () -> Unit = {},
    onEdit: (FullAccount) -> Unit = {},
    onDelete: (FullAccount) -> Unit = {},
    onSetFlag: (Long, Long?) -> Unit = { _, _ -> },
    onToggleSealed: (FullAccount) -> Unit = {},
    onToggleExcludeFromTotals: (FullAccount) -> Unit = {},
    onToggleDynamicExchangeRate: ((FullAccount) -> Unit)? = null,
    toggleExpansion: () -> Unit = { },
    bankIcon: @Composable ((Modifier, Long) -> Unit)? = null,
    flags: List<AccountFlag> = emptyList(),
) {
    val format = LocalCurrencyFormatter.current
    val showMenu = rememberSaveable { mutableStateOf(false) }
    val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)
    val homeCurrency = LocalHomeCurrency.current
    val showEquivalent = (showEquivalentWorth) || account.isHomeAggregate
    val currency = if (showEquivalent) homeCurrency else account.currencyUnit
    val currentBalance = format.convAmount(
        if (showEquivalent) account.equivalentCurrentBalance else account.currentBalance,
        currency
    )

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
                .size((dimensionResource(id = R.dimen.account_list_aggregate_letter_font_size).value * 2).dp)
            val color = Color(account.color(LocalResources.current))

            account.progress?.let { (sign, progress) ->
                DonutInABox(
                    modifier = modifier,
                    progress = progress,
                    fontSize = 10.sp,
                    color = color,
                    excessColor = LocalColors.current.amountColor(sign)
                )
            } ?: run {
                if (account.bankId == null || bankIcon == null) {
                    ColorCircle(modifier, color) {
                        if (account.isAggregate) {
                            Text(fontSize = 13.sp, text = "Σ", color = Color.White)
                        }
                    }
                } else bankIcon.invoke(modifier, account.bankId)
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
                        drawLine(
                            contentColor,
                            Offset(size.width / 5, size.height / 2),
                            Offset(size.width / 5 * 4, size.height / 2),
                            5f
                        )
                    },
                    imageVector = Icons.Filled.Functions,
                    contentDescription = stringResource(
                        id = R.string.menu_exclude_from_totals
                    )
                )
            }
            if (onToggleDynamicExchangeRate != null && account.dynamic) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ShowChart,
                    contentDescription = stringResource(
                        id = R.string.menu_exclude_from_totals
                    )
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = account.label,
                        maxLines = if (isCollapsed) 1 else Int.MAX_VALUE,
                        overflow = if (isCollapsed) TextOverflow.Ellipsis else TextOverflow.Clip
                    )
                    if (account.flag.icon != null) {
                        val contentDescription = account.flag.localizedLabel(LocalContext.current)
                        @Suppress("RemoveRedundantQualifierName")
                        org.totschnig.myexpenses.compose.Icon(
                            icon = account.flag.icon,
                            size = 12.sp,
                            modifier = Modifier.semantics {
                                this.contentDescription = contentDescription
                            }
                        )
                    }
                }
                AnimatedVisibility(visible = isCollapsed) {
                    Text(text = currentBalance)
                }
            }

            ExpansionHandle(
                isExpanded = !isCollapsed,
                contentDescription = account.label,
                toggle = toggleExpansion
            )
            val menu = Menu(
                buildList {
                    if (account.id > 0) {
                        if (!account.sealed) {
                            add(edit("EDIT_ACCOUNT") { onEdit(account) })
                        }
                        add(delete("DELETE_ACCOUNT") { onDelete(account) })
                        add(
                            SubMenuEntry(
                                icon = Icons.Filled.Flag,
                                label = R.string.menu_flag,
                                subMenu = Menu(
                                    flags.filter { it.id != DEFAULT_FLAG_ID }.map {
                                        val isChecked = account.flag.id == it.id
                                        CheckableMenuEntry(
                                            label = UiText.StringValue(it.localizedLabel(LocalContext.current)),
                                            command = "SET_FLAG",
                                            isRadio = true,
                                            isChecked = isChecked
                                        ) {
                                            if (isChecked) {
                                                onSetFlag(account.id, DEFAULT_FLAG_ID)
                                            } else
                                            onSetFlag(account.id, it.id)
                                        }
                                    }
                                )
                            )
                        )
                        add(
                            toggle("ACCOUNT", account.sealed) {
                                onToggleSealed(account)
                            }
                        )
                        add(
                            CheckableMenuEntry(
                                isChecked = account.excludeFromTotals,
                                label = R.string.menu_exclude_from_totals,
                                command = "EXCLUDE_FROM_TOTALS_COMMAND"
                            ) {
                                onToggleExcludeFromTotals(account)
                            })
                        onToggleDynamicExchangeRate
                            ?.takeIf { account.currency != homeCurrency.code }
                            ?.let {
                                add(
                                    CheckableMenuEntry(
                                        isChecked = account.dynamic,
                                        label = R.string.dynamic_exchange_rate,
                                        command = "DYNAMIC_EXCHANGE_RATE"
                                    ) {
                                        onToggleDynamicExchangeRate(account)
                                    })
                            }
                    }
                }
            )
            HierarchicalMenu(showMenu, menu)
        }

        val visibleState = remember { MutableTransitionState(!isCollapsed) }
        visibleState.targetState = !isCollapsed
        val borderColor = MaterialTheme.colorScheme.onSurface
        fun Modifier.drawSumLine() = drawBehind {
            val strokeWidth = 2 * density
            drawLine(
                borderColor,
                Offset(0f, 0f),
                Offset(size.width, 0f),
                strokeWidth
            )
        }

        val accountCurrencyIsolated = isolateText(account.currencyUnit.symbol)
        val homeCurrencyIsolated = isolateText(homeCurrency.symbol)

        AnimatedVisibility(visibleState) {
            Column(modifier = Modifier.padding(end = 16.dp)) {

                account.description?.let { Text(it) }
                val isFx = account.currency != homeCurrency.code

                val fXFormat = remember { DecimalFormat("#.############") }
                SumRow(
                    if (showEquivalent) R.string.initial_value else R.string.opening_balance,
                    format.convAmount(
                        if (showEquivalent) account.equivalentOpeningBalance else account.openingBalance,
                        currency
                    )
                )
                if (showEquivalent && isFx && account.equivalentOpeningBalance != 0L && account.initialExchangeRate != null) {
                    val realRate = calculateRealExchangeRate(
                        account.initialExchangeRate,
                        account.currencyUnit,
                        homeCurrency
                    )
                    Text("1 $accountCurrencyIsolated = ${fXFormat.format(realRate)} $homeCurrencyIsolated")
                }
                val displayIncome =
                    if (showEquivalent) account.equivalentSumIncome else account.sumIncome
                if (displayIncome != 0L) {
                    SumRow(
                        R.string.sum_income,
                        format.convAmount(displayIncome, currency)
                    )
                }
                val displayExpense =
                    if (showEquivalent) account.equivalentSumExpense else account.sumExpense
                if (displayExpense != 0L) {
                    SumRow(
                        R.string.sum_expenses,
                        format.convAmount(displayExpense, currency)
                    )
                }

                val displayTransfer =
                    if (showEquivalent) account.equivalentSumTransfer else account.sumTransfer

                if (displayTransfer != 0L) {
                    SumRow(
                        R.string.sum_transfer,
                        format.convAmount(displayTransfer, currency)
                    )
                }

                (if (showEquivalent) account.equivalentTotal else account.total)?.let {
                    SumRow(
                        if (showEquivalent) R.string.total_value else R.string.menu_aggregates,
                        format.convAmount(it, currency),
                        Modifier.drawSumLine()
                    )
                }

                SumRow(
                    if (showEquivalent) R.string.current_value else R.string.current_balance,
                    currentBalance,
                    Modifier.conditional(account.total == null) {
                        drawSumLine()
                    }
                )

                if (showEquivalent && (account.equivalentTotal != 0L || account.equivalentCurrentBalance != 0L)) {

                    account.latestExchangeRate?.let { (date, rate) ->
                        val dateFormatted = LocalDateFormatter.current.format(date)
                        val realRate =
                            calculateRealExchangeRate(rate, account.currencyUnit, homeCurrency)
                        Text(
                            "1 $accountCurrencyIsolated = ${fXFormat.format(realRate)} $homeCurrencyIsolated ($dateFormatted)"
                        )
                    }
                }

                account.criterion?.takeIf { !showEquivalent }?.let {
                    SumRow(
                        if (it > 0) R.string.saving_goal else R.string.credit_limit,
                        format.convAmount(it, account.currencyUnit)
                    )
                }

                if (!(showEquivalent || account.isAggregate || !account.type.supportsReconciliation)) {
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
        Text(
            stringResource(label),
            Modifier
                .weight(1f)
                .basicMarquee(iterations = 1),
            maxLines = 1
        )
        Text(formattedAmount, modifier)
    }
}

@Preview
@Composable
private fun AccountPreview() {
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
        ),
    )
}

@Preview
@Composable
fun MixedText() {
    val symbol = '﷼'
    Text("1 $symbol = 345 $")
}