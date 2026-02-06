package org.totschnig.myexpenses.compose.accounts

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.BalanceSheetOptions
import org.totschnig.myexpenses.activity.BalanceSheetViewInner
import org.totschnig.myexpenses.compose.CheckableMenuEntry
import org.totschnig.myexpenses.compose.TooltipIconMenu
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.compose.main.AppEvent
import org.totschnig.myexpenses.compose.main.AppEventHandler
import org.totschnig.myexpenses.compose.main.MyFloatingActionButton
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountGrouping
import org.totschnig.myexpenses.model.AccountGroupingKey
import org.totschnig.myexpenses.viewmodel.MyExpensesV2ViewModel
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import timber.log.Timber
import java.time.LocalDate

enum class AccountsScreenTab(@param:StringRes val resourceId: Int) {
    LIST(R.string.accounts),
    BALANCE_SHEET(R.string.balance_sheet)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountsScreen(
    startTab: AccountsScreenTab,
    navigationIcon: @Composable (() -> Unit) = {},
    accounts: List<FullAccount>,
    accountGrouping: AccountGrouping<*>,
    selectedAccountId: Long,
    viewModel: MyExpensesV2ViewModel,
    bottomBar: @Composable (() -> Unit),
    onEvent: AppEventHandler,
    flags: List<AccountFlag> = emptyList(),
    onAccountEvent: AccountEventHandler,
    bankIcon: (@Composable (Modifier, Long) -> Unit)? = null,
    onNavigateToTransactions: () -> Unit
) {

    val selectedTab = rememberSaveable { mutableStateOf(startTab) }

    LaunchedEffect(selectedTab.value) {
        viewModel.setLastVisited(selectedTab.value)
    }

    val showHiddenState = viewModel.balanceSheetShowHidden.asState()
    val showZeroState = viewModel.balanceSheetShowZero.asState()
    val showChartState = viewModel.balanceSheetShowChart.asState()
    val highlight = remember { mutableStateOf<Triple<Boolean, Int, Long?>?>(null) }

    fun navigateToAccount(id: Long) {

        viewModel.selectAccount(id)
        onNavigateToTransactions()
    }

    fun navigateToGroup(id: AccountGroupingKey) {
        viewModel.navigateToGroup(id)
        onNavigateToTransactions()
    }


    Scaffold(
        topBar = {
            TopAppBar(
                navigationIcon = navigationIcon,
                title = {
                    Title(selectedTab.value) {
                        selectedTab.value = it
                    }
                },
                actions = {
                    when (selectedTab.value) {
                        AccountsScreenTab.LIST -> {
                            AccountGroupingMenu(
                                activeGrouping = accountGrouping,
                                onGroupingChange = { onEvent(AppEvent.SetAccountGrouping(it)) },
                            )
                        }

                        AccountsScreenTab.BALANCE_SHEET -> {
                            val accounts =
                                viewModel.accountsForBalanceSheet.collectAsState(LocalDate.now() to emptyList()).value.second
                            BalanceSheetOptions(
                                showHiddenState.takeIf { accounts.any { !it.isVisible } },
                                showZeroState.takeIf { accounts.any { it.currentBalance == 0L } },
                                showChartState,
                                highlight,
                                onPrint = { onEvent(AppEvent.PrintBalanceSheet) }
                            )
                        }
                    }

                }
            )
        },
        bottomBar = bottomBar,
        floatingActionButton = {
            MyFloatingActionButton(
                onClick = { onEvent(AppEvent.CreateAccount) },
                contentDescription = "Add Account"
            )
        }
    ) { paddingValues ->
        if (selectedTab.value == AccountsScreenTab.BALANCE_SHEET) {
            val data =
                viewModel.accountsForBalanceSheet.collectAsState(LocalDate.now() to emptyList()).value
            Column(modifier = Modifier.padding(paddingValues)) {
                BalanceSheetViewInner(
                    accounts = data.second,
                    debtSum = viewModel.debtSum.collectAsState(0).value,
                    date = data.first,
                    onNavigate = {
                        if (it.isVisible) {
                            if (accountGrouping != AccountGrouping.NONE) {
                                viewModel.maybeResetFilter(accountGrouping.getGroupKey(it))
                            }
                        } else {
                            //by setting the transaction screen to filter by the invisible flag,
                            //we make sure that account is displayed
                            viewModel.setGrouping(AccountGrouping.FLAG)
                            viewModel.setFilter(it.flag)
                        }
                        navigateToAccount(it.id)
                    },
                    onSetDate = {
                        viewModel.setBalanceDate(it)
                    },
                    showChart = showChartState.value,
                    highlight = highlight,
                    showHidden = showHiddenState.value,
                    showZero = showZeroState.value,
                    bottomPadding = dimensionResource(R.dimen.fab_related_bottom_padding)
                )
            }
        } else {
            AccountListV2(
                accountData = accounts,
                scaffoldPadding = paddingValues,
                grouping = accountGrouping,
                selectedAccount = selectedAccountId,
                listState = viewModel.listState,
                expansionHandlerGroups = viewModel.expansionHandler("collapsedHeadersDrawer_${accountGrouping}"),
                onSelected = {
                    if (accountGrouping != AccountGrouping.NONE) {
                        viewModel.maybeResetFilter(accountGrouping.getGroupKey(it))
                    }
                    navigateToAccount(it.id)
                },
                onGroupSelected = ::navigateToGroup,
                onEvent = onAccountEvent,
                flags = flags,
                bankIcon = bankIcon
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun Title(
    selectedTab: AccountsScreenTab,
    setSelectedTab: (AccountsScreenTab) -> Unit = {},
) {
    var isDropdownExpanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = isDropdownExpanded,
        onExpandedChange = { isDropdownExpanded = !isDropdownExpanded }
    ) {

        val textMeasurer = rememberTextMeasurer()
        val style = LocalTextStyle.current
        val options = AccountsScreenTab.entries.map { stringResource(it.resourceId) }
        val maxTextWidth = remember {
            // Get the string for all possible options
            // Measure each one and find the maximum width
            options.maxOf { text ->
                textMeasurer.measure(text, style).size.width
            }.also {
                Timber.d("Max text width: $it")
            }
        }

        TextField(
            state = TextFieldState(stringResource(selectedTab.resourceId)),
            readOnly = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
            ),
            // The dropdown arrow
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = isDropdownExpanded)
            },
            contentPadding = PaddingValues(
                start = 0.dp,
                end = 0.dp,
                top = 16.dp,
                bottom = 16.dp
            ),
            modifier = Modifier
                //48.dp is space for the drop down
                .width(with(LocalDensity.current) { maxTextWidth.toDp() + 48.dp })
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
        )

        // This is the actual dropdown menu that appears.
        ExposedDropdownMenu(
            expanded = isDropdownExpanded,
            onDismissRequest = { isDropdownExpanded = false }
        ) {
            AccountsScreenTab.entries.forEach { tab ->
                DropdownMenuItem(
                    text = { Text(stringResource(tab.resourceId)) },
                    onClick = {
                        setSelectedTab(tab)
                        isDropdownExpanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun AccountGroupingMenu(
    activeGrouping: AccountGrouping<*>,
    onGroupingChange: (AccountGrouping<*>) -> Unit,
) {
    val groupingOptions = remember { AccountGrouping.ALL_VALUES }

    TooltipIconMenu(
        tooltip = stringResource(R.string.menu_grouping),
        imageVector = Icons.Default.GridView,
        menu = groupingOptions.map { option ->
            CheckableMenuEntry(
                label = UiText.StringValue(option.toString()),
                action = { onGroupingChange(option) },
                command = "CHANGE_GROUPING",
                isChecked = activeGrouping == option,
                isRadio = true
            )
        }
    )
}

@Preview
@Composable
fun TitlePreview() {
    Title(AccountsScreenTab.LIST)
}

