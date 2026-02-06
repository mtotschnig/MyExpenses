package org.totschnig.myexpenses.compose.accounts

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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.CheckableMenuEntry
import org.totschnig.myexpenses.compose.ColorCircle
import org.totschnig.myexpenses.compose.DonutInABox
import org.totschnig.myexpenses.compose.ExpansionHandle
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.compose.LocalHomeCurrency
import org.totschnig.myexpenses.compose.MenuEntry.Companion.delete
import org.totschnig.myexpenses.compose.MenuEntry.Companion.edit
import org.totschnig.myexpenses.compose.MenuEntry.Companion.toggle
import org.totschnig.myexpenses.compose.OverFlowMenu
import org.totschnig.myexpenses.compose.SubMenuEntry
import org.totschnig.myexpenses.compose.TEST_TAG_ACCOUNTS
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.compose.optional
import org.totschnig.myexpenses.compose.scrollbar.LazyColumnWithScrollbar
import org.totschnig.myexpenses.compose.scrollbar.LazyColumnWithScrollbarAndBottomPadding
import org.totschnig.myexpenses.compose.transactions.BalanceType
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.DEFAULT_FLAG_ID
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.AGGREGATE_HOME_CURRENCY_CODE
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import org.totschnig.myexpenses.util.convAmount
import org.totschnig.myexpenses.util.isolateText
import org.totschnig.myexpenses.viewmodel.data.AggregateAccount
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import java.text.DecimalFormat

const val SIGMA = "Σ"

sealed class AccountEvent {
    data object Edit : AccountEvent()
    data object Delete : AccountEvent()
    data class SetFlag(val flagId: Long) : AccountEvent()
    data object ToggleSealed : AccountEvent()
    data object ToggleExcludeFromTotals : AccountEvent()
    data object ToggleDynamicExchangeRate : AccountEvent()
}

interface AccountEventHandler {
    operator fun invoke(event: AccountEvent, account: FullAccount)
}

@Composable
fun AccountList(
    modifier: Modifier = Modifier,
    accountData: List<FullAccount>,
    grouping: AccountGrouping<*>,
    selectedAccount: Long,
    listState: LazyListState,
    showEquivalentWorth: Boolean = false,
    onSelected: (Long) -> Unit = {},
    onEdit: (FullAccount) -> Unit = {},
    onDelete: (FullAccount) -> Unit = {},
    onSetFlag: (Long, Long) -> Unit = { _, _ -> },
    onToggleSealed: (FullAccount) -> Unit = {},
    onToggleExcludeFromTotals: (FullAccount) -> Unit = {},
    onToggleDynamicExchangeRate: ((FullAccount) -> Unit)? = null,
    flags: List<AccountFlag> = emptyList(),
    expansionHandlerGroups: org.totschnig.myexpenses.compose.ExpansionHandler,
    expansionHandlerAccounts: org.totschnig.myexpenses.compose.ExpansionHandler,
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
) {
    val context = LocalContext.current
    val collapsedGroupIds = expansionHandlerGroups.state.collectAsState(initial = null).value
    val expandedAccountIds =
        expansionHandlerAccounts.state.collectAsState(initial = null).value

    if (collapsedGroupIds != null && expandedAccountIds != null) {
        val grouped: Map<String, List<FullAccount>> =
            accountData.groupBy { getHeaderId(grouping, it) }
        LazyColumnWithScrollbarAndBottomPadding(
            modifier = modifier.background(MaterialTheme.colorScheme.background),
            state = listState,
            itemsAvailable = accountData.size + grouped.size,
            withFab = false,
            testTag = TEST_TAG_ACCOUNTS,
        ) {
            grouped.forEach { group ->
                val headerId = group.key
                val isGroupHidden = collapsedGroupIds.contains(headerId)
                item {
                    Header(
                        header = getHeaderTitle(
                            context = context,
                            grouping = grouping,
                            account = group.value.first()
                        ),
                        isExpanded = !isGroupHidden,
                        onToggleExpand = { expansionHandlerGroups.toggle(headerId) }
                    )
                }
                if (!isGroupHidden) {
                    group.value.forEachIndexed { index, account ->
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
fun AccountListV2(
    modifier: Modifier = Modifier,
    scaffoldPadding: PaddingValues = PaddingValues(0.dp),
    accountData: List<FullAccount>,
    grouping: AccountGrouping<*>,
    selectedAccount: Long,
    listState: LazyListState,
    expansionHandlerGroups: org.totschnig.myexpenses.compose.ExpansionHandler,
    onSelected: (FullAccount) -> Unit = {},
    onGroupSelected: (AccountGroupingKey) -> Unit = {},
    onEvent: AccountEventHandler,
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
    flags: List<AccountFlag> = emptyList(),
) {

    val context = LocalContext.current
    val collapsedGroupIds =
        expansionHandlerGroups.state.collectAsState(initial = null).value ?: return

    val grouped: Map<AccountGroupingKey, List<FullAccount>> =
        accountData.groupBy { grouping.getGroupKey(it) }

    val sortedGroupKeys = remember(grouped, grouping) {
        grouped.keys.sortedWith(grouping.comparator as Comparator<in AccountGroupingKey>)
    }

    LazyColumnWithScrollbar(
        modifier = modifier,
        state = listState,
        itemsAvailable = accountData.size + grouped.size,
        testTag = TEST_TAG_ACCOUNTS,
        contentPadding = PaddingValues(
            top = scaffoldPadding.calculateTopPadding(),
            bottom = scaffoldPadding.calculateBottomPadding() + dimensionResource(R.dimen.fab_related_bottom_padding)
        )
    ) {
        sortedGroupKeys.forEach { groupKey ->
            val group = grouped.getValue(groupKey)
            val headerId = groupKey.id.toString()
            val isGroupHidden = collapsedGroupIds.contains(headerId)
            item {
                HeaderV2(
                    header = groupKey.title(context),
                    currency = groupKey as? CurrencyUnit
                        ?: LocalHomeCurrency.current,
                    total = group.filter { !it.excludeFromTotals }.sumOf {
                        if (grouping == AccountGrouping.CURRENCY) it.currentBalance else it.equivalentCurrentBalance
                    },
                    onToggleExpand = { expansionHandlerGroups.toggle(headerId) },
                    onNavigate = if (group.size < 2) null else {
                        { onGroupSelected(groupKey) }
                    }
                )
            }
            if (!isGroupHidden) {
                //if we group by flag we also show the members of a group that is configured as invisible
                (if (grouping == AccountGrouping.FLAG) group else group.filter { it.visible }).forEach { account ->
                    item(key = account.id) {
                        AccountCardV2(
                            account,
                            isSelected = account.id == selectedAccount,
                            onSelected = { onSelected(account) },
                            onEvent = onEvent,
                            flags = flags,
                            bankIcon = bankIcon
                        )
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
    onToggleExpand: () -> Unit,
    onNavigate: (() -> Unit)? = null,
) {
    HorizontalDivider(
        color = colorResource(id = androidx.appcompat.R.color.material_grey_300),
        thickness = 2.dp
    )
    Row(
        modifier = Modifier
            .clickable(onClick = onNavigate ?: onToggleExpand)
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
                .conditional(onNavigate != null) {
                    clickable(onClick = onToggleExpand)
                }
                .rotate(rotationAngle),
            imageVector = Icons.Default.ExpandLess,
            contentDescription = stringResource(
                id = if (isExpanded) R.string.collapse
                else R.string.expand
            )
        )
    }
}

@Composable
private fun HeaderV2(
    header: String,
    onToggleExpand: () -> Unit,
    onNavigate: (() -> Unit)?,
    total: Long,
    currency: CurrencyUnit,
) {

    val format = LocalCurrencyFormatter.current
    HorizontalDivider(
        color = colorResource(id = androidx.appcompat.R.color.material_grey_300),
        thickness = 2.dp
    )
    Row(
        modifier = Modifier
            .clickable(onClick = onToggleExpand)
            .heightIn(min = 48.dp)
            .semantics(mergeDescendants = true) {}
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier.weight(1f),
            text = header,
            style = MaterialTheme.typography.titleMedium,
            color = colorResource(id = R.color.material_grey)
        )
        Text(format.convAmount(total, currency))
        if (onNavigate == null) {
            Spacer(Modifier.width(48.dp))
        } else {
            IconButton(onClick = onNavigate) {
                Icon(
                    Icons.Filled.ChevronRight,
                    "VIEW ALL"
                )
            }
        }
    }
}

private fun getHeaderId(
    grouping: AccountGrouping<*>,
    account: FullAccount,
) = when (grouping) {
    AccountGrouping.NONE -> if (account.id > 0) "0" else "1"

    AccountGrouping.TYPE -> account.type.name

    AccountGrouping.CURRENCY ->
        if (account.id == HOME_AGGREGATE_ID) AGGREGATE_HOME_CURRENCY_CODE else account.currency

    AccountGrouping.FLAG -> account.flag.label
}

private fun getHeaderTitle(
    context: Context,
    grouping: AccountGrouping<*>,
    account: FullAccount,
) = when (grouping) {
    AccountGrouping.NONE -> context.getString(
        if (account.id > 0) R.string.pref_manage_accounts_title else R.string.menu_aggregates
    )

    AccountGrouping.TYPE ->
        if (account.isAggregate) context.getString(R.string.menu_aggregates) else
            account.type.title(context)

    AccountGrouping.CURRENCY -> if (account.id == HOME_AGGREGATE_ID)
        context.getString(R.string.menu_aggregates)
    else
        Currency.create(account.currency, context).toString()

    AccountGrouping.FLAG -> account.flag.title(context)
}

@Composable
fun AccountCardV2(
    account: FullAccount,
    isSelected: Boolean = false,
    onSelected: () -> Unit = {},
    onEvent: AccountEventHandler,
    flags: List<AccountFlag> = emptyList(),
    bankIcon: @Composable ((Modifier, Long) -> Unit)? = null,
) {

    val format = LocalCurrencyFormatter.current
    val activatedBackgroundColor = colorResource(id = R.color.activatedBackground)

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .conditional(isSelected) {
                background(activatedBackgroundColor)
            }
            .clickable(onClick = onSelected)
            .padding(end = 4.dp, start = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AccountIndicator(
            account = account,
            bankIcon = bankIcon
        )
        Text(text = account.label, modifier = Modifier.weight(1f))
        Text(format.convAmount(account.currentBalance, account.currencyUnit))
        OverFlowMenu(
            menu = accountMenu(
                context = LocalContext.current,
                homeCurrency = LocalHomeCurrency.current,
                account = account,
                onEvent = onEvent,
                flags = flags
            )
        )
    }
}

@Composable
fun AccountIndicator(
    size: Dp = (dimensionResource(id = R.dimen.account_list_aggregate_letter_font_size).value * 2).dp,
    account: FullAccount,
    bankIcon: @Composable ((Modifier, Long) -> Unit)?,
) {
    val color = Color(account.color(resources = LocalResources.current))

    val modifier = Modifier
        .padding(end = 6.dp)
        .size(size)

    account.progress?.let { (sign, progress) ->
        DonutInABox(
            modifier = modifier,
            progress = progress,
            fontSize = 10.sp,
            color = color,
            excessColor = LocalColors.current.amountColor(
                sign
            )
        )
    } ?: run {
        if (account.bankId == null || bankIcon == null) {
            ColorCircle(
                modifier,
                color
            ) {
                if (account.isAggregate) {
                    Text(fontSize = 13.sp, text = SIGMA, color = Color.White)
                }
            }
        } else bankIcon.invoke(modifier, account.bankId)
    }
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
    onSetFlag: (Long, Long) -> Unit = { _, _ -> },
    onToggleSealed: (FullAccount) -> Unit = {},
    onToggleExcludeFromTotals: (FullAccount) -> Unit = {},
    onToggleDynamicExchangeRate: ((FullAccount) -> Unit)? = null,
    flags: List<AccountFlag> = emptyList(),
    toggleExpansion: () -> Unit = { },
    bankIcon: @Composable ((Modifier, Long) -> Unit)? = null,
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
            AccountIndicator(
                account = account,
                bankIcon = bankIcon
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
                        val contentDescription = account.flag.title(LocalContext.current)
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

            HierarchicalMenu(
                showMenu, accountMenu(
                    context = LocalContext.current,
                    homeCurrency = homeCurrency,
                    account = account,
                    onEvent = object : AccountEventHandler {
                        override fun invoke(event: AccountEvent, account: FullAccount) {
                            when (event) {
                                is AccountEvent.Delete -> onDelete(account)

                                is AccountEvent.Edit -> onEdit(account)

                                is AccountEvent.SetFlag -> onSetFlag(account.id, event.flagId)

                                is AccountEvent.ToggleDynamicExchangeRate ->
                                    onToggleDynamicExchangeRate?.invoke(account)

                                is AccountEvent.ToggleExcludeFromTotals ->
                                    onToggleExcludeFromTotals(account)

                                is AccountEvent.ToggleSealed -> onToggleSealed(account)

                            }
                        }
                    },
                    flags = flags
                )
            )
        }

        val visibleState = remember { MutableTransitionState(!isCollapsed) }
        visibleState.targetState = !isCollapsed

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
                        val dateFormatted =
                            LocalDateFormatter.current.format(
                                date
                            )
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

@Composable
fun AccountSummaryV2(
    account: BaseAccount,
    displayBalanceType: BalanceType,
    onDisplayBalanceTypeChange: (BalanceType) -> Unit,
) {
    Column(modifier = Modifier.padding(16.dp)) {
        when (account) {
            is FullAccount -> AccountSummaryV2(
                account,
                displayBalanceType,
                onDisplayBalanceTypeChange
            )

            is AggregateAccount -> AccountSummaryV2(
                account,
                displayBalanceType,
                onDisplayBalanceTypeChange
            )
        }
    }
}

@Composable
fun AccountSummaryV2(
    account: FullAccount,
    displayBalanceType: BalanceType,
    onDisplayBalanceTypeChange: (BalanceType) -> Unit,
) {
    val homeCurrency = LocalHomeCurrency.current
    val isFx = account.currency != homeCurrency.code
    val fXFormat = remember { DecimalFormat("#.############") }

    SumRowV2(
        label = R.string.opening_balance,
        amount = account.openingBalance,
        currency = account.currencyUnit,
        formattedEquivalentAmount = account.equivalentOpeningBalance.takeIf { isFx }
    )

    if (account.sumIncome != 0L) {
        SumRowV2(
            label = R.string.sum_income,
            amount = account.sumIncome,
            currency = account.currencyUnit,
            formattedEquivalentAmount = account.equivalentSumIncome.takeIf { isFx }
        )
    }

    if (account.sumExpense != 0L) {
        SumRowV2(
            label = R.string.sum_expenses,
            amount = account.sumExpense,
            currency = account.currencyUnit,
            formattedEquivalentAmount = account.equivalentSumExpense.takeIf { isFx }
        )
    }

    if (account.sumTransfer != 0L) {
        SumRowV2(
            label = R.string.sum_transfer,
            amount = account.sumTransfer,
            currency = account.currencyUnit,
            formattedEquivalentAmount = account.equivalentSumTransfer.takeIf { isFx }
        )
    }

    account.total?.let {
        SumRowV2(
            label = R.string.menu_aggregates,
            amount = account.total,
            currency = account.currencyUnit,
            modifier = Modifier.drawSumLine(),
            formattedEquivalentAmount = account.equivalentTotal.takeIf { isFx },
            highlight = displayBalanceType == BalanceType.TOTAL
        ) { onDisplayBalanceTypeChange(BalanceType.TOTAL) }
    }

    SumRowV2(
        label = R.string.current_balance,
        amount = account.currentBalance,
        currency = account.currencyUnit,
        modifier = Modifier.conditional(account.total == null) {
            drawSumLine()
        },
        formattedEquivalentAmount = account.equivalentCurrentBalance.takeIf { isFx },
        highlight = displayBalanceType == BalanceType.CURRENT
    ) { onDisplayBalanceTypeChange(BalanceType.CURRENT) }

    account.criterion?.let {
        SumRowV2(
            label = if (it > 0) R.string.saving_goal else R.string.credit_limit,
            amount = it,
            currency = account.currencyUnit,
        )
    }

    if (account.type.supportsReconciliation) {
        SumRowV2(
            label = R.string.total_cleared,
            amount = account.clearedTotal,
            currency = account.currencyUnit,
            highlight = displayBalanceType == BalanceType.CLEARED,
        ) { onDisplayBalanceTypeChange(BalanceType.CLEARED) }
        SumRowV2(
            label = R.string.total_reconciled,
            amount = account.reconciledTotal,
            currency = account.currencyUnit,
            highlight = displayBalanceType == BalanceType.RECONCILED,
        ) { onDisplayBalanceTypeChange(BalanceType.RECONCILED) }
    }
}

@Composable
fun AccountSummaryV2(
    account: AggregateAccount,
    displayBalanceType: BalanceType,
    onDisplayBalanceTypeChange: (BalanceType) -> Unit,
) {
    val homeCurrency = LocalHomeCurrency.current
    val isFx = account.currency != homeCurrency.code

    SumRowV2(
        label = R.string.opening_balance,
        amount = account.openingBalance ?: account.equivalentOpeningBalance,
        currency = account.currencyUnit,
        formattedEquivalentAmount = account.equivalentOpeningBalance.takeIf { isFx }
    )

    (account.sumIncome ?: account.equivalentSumIncome).takeIf { it != 0L }?.let {
        SumRowV2(
            label = R.string.sum_income,
            amount = it,
            currency = account.currencyUnit,
            formattedEquivalentAmount = account.equivalentSumIncome.takeIf { isFx }
        )
    }

    (account.sumExpense ?: account.equivalentSumExpense).takeIf { it != 0L }?.let {
        SumRowV2(
            label = R.string.sum_expenses,
            amount = it,
            currency = account.currencyUnit,
            formattedEquivalentAmount = account.equivalentSumExpense.takeIf { isFx }
        )
    }

    (account.sumTransfer ?: account.equivalentSumTransfer).takeIf { it != 0L }?.let {
        SumRowV2(
            label = R.string.sum_transfer,
            amount = it,
            currency = account.currencyUnit,
            formattedEquivalentAmount = account.equivalentSumTransfer.takeIf { isFx }
        )
    }

    val displayTotal = account.total ?: account.equivalentTotal.takeIf { it != 0L }
    displayTotal?.let {
        SumRowV2(
            label = R.string.menu_aggregates,
            amount = it,
            currency = account.currencyUnit,
            modifier = Modifier.drawSumLine(),
            formattedEquivalentAmount = account.equivalentTotal.takeIf { isFx },
            highlight = displayBalanceType == BalanceType.TOTAL
        ) { onDisplayBalanceTypeChange(BalanceType.TOTAL) }
    }

    SumRowV2(
        label = R.string.current_balance,
        amount = account.currentBalance ?: account.equivalentCurrentBalance,
        currency = account.currencyUnit,
        modifier = Modifier.conditional(displayTotal == null) {
            drawSumLine()
        },
        formattedEquivalentAmount = account.equivalentCurrentBalance.takeIf { isFx },
        highlight = displayBalanceType == BalanceType.CURRENT
    ) { onDisplayBalanceTypeChange(BalanceType.CURRENT) }
}


private fun accountMenu(
    context: Context,
    homeCurrency: CurrencyUnit,
    account: FullAccount,
    onEvent: AccountEventHandler,
    flags: List<AccountFlag>,
) = buildList {
    if (account.id > 0) {
        if (!account.sealed) {
            add(edit("EDIT_ACCOUNT") { onEvent(AccountEvent.Edit, account) })
        }
        add(delete("DELETE_ACCOUNT") { onEvent(AccountEvent.Delete, account) })
        add(
            SubMenuEntry(
                label = R.string.menu_flag,
                icon = Icons.Filled.Flag,
                subMenu = flags.filter { it.id != DEFAULT_FLAG_ID }.map {
                    val isChecked = account.flag.id == it.id
                    CheckableMenuEntry(
                        label = UiText.StringValue(
                            it.title(context)
                        ),
                        command = "SET_FLAG",
                        isRadio = true,
                        isChecked = isChecked
                    ) {
                        onEvent(
                            AccountEvent.SetFlag(if (isChecked) DEFAULT_FLAG_ID else it.id),
                            account
                        )
                    }
                }
            )
        )
        add(
            toggle("ACCOUNT", account.sealed) {
                onEvent(AccountEvent.ToggleSealed, account)
            }
        )
        add(
            CheckableMenuEntry(
                isChecked = account.excludeFromTotals,
                label = R.string.menu_exclude_from_totals,
                command = "EXCLUDE_FROM_TOTALS_COMMAND"
            ) {
                onEvent(AccountEvent.ToggleExcludeFromTotals, account)
            })
        if (account.currency != homeCurrency.code) {
            add(
                CheckableMenuEntry(
                    isChecked = account.dynamic,
                    label = R.string.dynamic_exchange_rate,
                    command = "DYNAMIC_EXCHANGE_RATE"
                ) {
                    onEvent(AccountEvent.ToggleDynamicExchangeRate, account)
                })
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

@Composable
fun SumRowV2(
    label: Int,
    amount: Long,
    currency: CurrencyUnit,
    modifier: Modifier = Modifier,
    formattedEquivalentAmount: Long? = null,
    highlight: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val fontWeight = if (highlight) FontWeight.Bold else null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .optional(onClick) { clickable(onClick = it) },
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = stringResource(label),
            modifier = Modifier.weight(1f),
            maxLines = 1,
            fontWeight = fontWeight,
        )
        Column(modifier, horizontalAlignment = Alignment.End) {
            AmountText(amount, currency)
            formattedEquivalentAmount?.let {
                AmountText(
                    it,
                    LocalHomeCurrency.current,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun Modifier.drawSumLine(): Modifier {
    val borderColor = MaterialTheme.colorScheme.onSurface
    return drawBehind {
        val strokeWidth = 2 * density
        drawLine(
            borderColor,
            Offset(0f, 0f),
            Offset(size.width, 0f),
            strokeWidth
        )
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
            color = android.graphics.Color.RED,
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
private fun AccountPreview2() {
    AccountCardV2(
        account = FullAccount(
            id = 1,
            label = "Account",
            description = "Description",
            currencyUnit = CurrencyUnit.DebugInstance,
            color = android.graphics.Color.RED,
            openingBalance = 0,
            currentBalance = 1000,
            sumIncome = 2000,
            sumExpense = 1000,
            sealed = true,
            type = AccountType.CASH,
            criterion = 5000,
            excludeFromTotals = true
        ),
        onEvent = object : AccountEventHandler {
            override fun invoke(
                event: AccountEvent,
                account: FullAccount
            ) {
            }
        }
    )
}

@Preview
@Composable
fun MixedText() {
    val symbol = '﷼'
    Text("1 $symbol = 345 $")
}