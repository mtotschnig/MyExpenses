package org.totschnig.myexpenses.activity

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DisplayMode
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AmountText
import org.totschnig.myexpenses.compose.CheckableMenuEntry
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.LocalDateFormatter
import org.totschnig.myexpenses.compose.LocalHomeCurrency
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.filter.ActionButton
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.epochMillis2LocalDate
import org.totschnig.myexpenses.util.toEpoch
import org.totschnig.myexpenses.viewmodel.data.BalanceAccount
import java.time.LocalDate
import java.util.Currency
import kotlin.math.absoluteValue
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceSheetView(
    accounts: List<BalanceAccount>,
    date: LocalDate = LocalDate.now(),
    onClose: () -> Unit = {},
    onNavigate: (Long) -> Unit = {},
    onSetDate: (LocalDate) -> Unit = {},
) {
    var showAll by rememberSaveable { mutableStateOf(true) }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = System.currentTimeMillis()
    )

    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    val horizontalPadding = dimensionResource(R.dimen.padding_main_screen)

    Column {
        TopAppBar(
            title = {
                Text(text = stringResource(R.string.balance_sheet))
            },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            },
            actions = {
                val expanded = rememberSaveable { mutableStateOf(false) }
                IconButton(onClick = { expanded.value = true }) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Settings,
                        contentDescription = "Options"
                    )
                }
                HierarchicalMenu(
                    expanded, Menu(
                        listOf(
                            CheckableMenuEntry(
                                label = R.string.show_all,
                                command = "TOGGLE_SHOW_ALL",
                                showAll
                            ) {
                                showAll = !showAll
                            }
                        )))
            },
            colors = TopAppBarDefaults.topAppBarColors()
        )

        Text(
            modifier = Modifier
                .padding(end = horizontalPadding)
                .clickable {
                    datePickerState.selectedDateMillis = date.toEpoch() * 1000
                    showDatePicker = true
                }
                .align(Alignment.End),
            text = LocalDateFormatter.current.format(date),
            textDecoration = TextDecoration.Underline
        )
        if (showDatePicker) {
            Popup(
                properties = PopupProperties(focusable = true),
                onDismissRequest = {
                    showDatePicker = false
                }
            ) {

                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .offset(y = 64.dp)
                        .shadow(elevation = 4.dp)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(16.dp)
                ) {
                    val isNarrow = this@BoxWithConstraints.maxWidth < 360.dp
                    LaunchedEffect(LocalConfiguration.current) {
                        if (isNarrow) {
                            datePickerState.displayMode = DisplayMode.Input
                        }
                    }
                    DatePicker(
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                        state = datePickerState,
                        showModeToggle = !isNarrow
                    )
                    ActionButton(
                        modifier = Modifier.align(Alignment.TopEnd),
                        hintText = stringResource(R.string.confirm),
                        icon = Icons.Filled.Done
                    ) {
                        showDatePicker = false
                        datePickerState.selectedDateMillis?.let {
                            onSetDate(epochMillis2LocalDate(it))
                        }
                    }
                }
            }
        }

        val (assets, liabilities) = accounts
            .groupBy { it.type }
            .mapValues { it.value.sumOf { it.equivalentCurrentBalance } to it.value }
            .entries
            .partition { it.key.isAsset }
        val totalAssets = assets.sumOf { it.value.first }
        val totalLiabilities = liabilities.sumOf { it.value.first }

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = horizontalPadding)
        ) {

            accountTypeChapter(
                R.string.balance_sheet_section_assets,
                totalAssets,
                assets,
                showAll,
                onNavigate
            )
            accountTypeChapter(
                R.string.balance_sheet_section_liabilities,
                totalLiabilities,
                liabilities,
                showAll,
                onNavigate
            )

            item {
                NetWorthView(totalAssets + totalLiabilities)
            }

        }
    }
}

fun LazyListScope.accountTypeChapter(
    @StringRes title: Int,
    total: Long,
    sections: List<Map.Entry<AccountType, Pair<Long, List<BalanceAccount>>>>,
    showAll: Boolean,
    onNavigate: (Long) -> Unit,
) {
    item {
        BalanceSheetSectionHeaderView(
            name = stringResource(title),
            total = total
        )
    }

    sections.forEach { (type, group) ->
        accountTypeSection(type = type, group, showAll, onNavigate)
    }
}

@Composable
fun BalanceSheetSectionHeaderView(name: String, total: Long) {
    val homeCurrency = LocalHomeCurrency.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        ColoredAmountText(total, homeCurrency, absolute = true)
    }
}

fun LazyListScope.accountTypeSection(
    type: AccountType,
    group: Pair<Long, List<BalanceAccount>>,
    showAll: Boolean,
    onNavigate: (Long) -> Unit,
) {
    val (total, accounts) = group
    item {
        val homeCurrency = LocalHomeCurrency.current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(
                text = stringResource(type.toStringResPlural()), // Display the AccountType name
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            ColoredAmountText(total, homeCurrency, absolute = true)
        }
    }
    accounts
        .filter { showAll || (!it.isHidden && it.currentBalance != 0L) }
        .forEach { account ->
            item {
                BalanceAccountItemView(account = account, onNavigate)
            }
        }
    item {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun BalanceAccountItemView(account: BalanceAccount, onNavigate: (Long) -> Unit) {
    val homeCurrency = LocalHomeCurrency.current
    Row(
        modifier = Modifier
            .clickable {
                onNavigate(account.id)
            }
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = account.label,
            modifier = Modifier.weight(1f)
        )
        Column(horizontalAlignment = Alignment.End) {
            ColoredAmountText(account.equivalentCurrentBalance, homeCurrency, absolute = true)
            if (account.currency.code != homeCurrency.code && account.currentBalance != 0L) {
                AmountText(
                    account.currentBalance.absoluteValue,
                    account.currency,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun NetWorthView(netWorth: Long) {
    val homeCurrency = LocalHomeCurrency.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Text(
            text = stringResource(R.string.balance_sheet_net_worth),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        ColoredAmountText(netWorth, homeCurrency, fontWeight = FontWeight.Bold)
    }
}

@Preview
@Composable
fun BalanceSheet() {
    BalanceSheetView(
        listOf(
            BalanceAccount(
                label = "Checking Account",
                type = AccountType.CASH,
                currentBalance = 500000, // $5,000.00 (assuming cents)
            ),
            BalanceAccount(
                label = "Savings Account",
                type = AccountType.BANK,
                currentBalance = 1250000, // $12,500.00
            ),
            BalanceAccount(
                label = "Credit Card 1",
                type = AccountType.CCARD,
                currentBalance = -25000, // -$250.00 (negative balance for debt)
            ),
            BalanceAccount(
                label = "Credit Card 2",
                type = AccountType.CCARD,
                currentBalance = -100000, // -$1,000.00
            ),
            BalanceAccount(
                label = "USD Cash",
                type = AccountType.CASH,
                currentBalance = 200000, // €2,000.00
                currency = CurrencyUnit(Currency.getInstance("USD")),
                equivalentCurrentBalance = (200000 * 0.92).roundToLong()
            ),
            BalanceAccount(
                label = "JPY Account",
                type = AccountType.BANK,
                currentBalance = 5000000, // ¥50,000.00
                currency = CurrencyUnit(Currency.getInstance("JPY")),
                equivalentCurrentBalance = (5000000 * 0.0075).roundToLong(),
            ),
            BalanceAccount(
                label = "USD Invest Account",
                type = AccountType.ASSET,
                currentBalance = 2000000, // $20,000
            ),
            BalanceAccount(
                label = "GBP Cash",
                type = AccountType.CASH,
                currentBalance = 100000, // £1,000.00
                currency = CurrencyUnit(Currency.getInstance("GBP")),
                equivalentCurrentBalance = (100000 * 1.28).roundToLong(),
            ),
            BalanceAccount(
                label = "Loan Account",
                type = AccountType.LIABILITY,
                currentBalance = -1000000, // -$10,000.00 (loan debt)
            ),
            BalanceAccount(
                label = "Petty Cash",
                type = AccountType.CASH,
                currentBalance = 10000, // $100.00 (loan debt)
            )
        )
    )
}

