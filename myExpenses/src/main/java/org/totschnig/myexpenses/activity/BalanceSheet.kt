package org.totschnig.myexpenses.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ColoredAmountText
import org.totschnig.myexpenses.compose.LocalHomeCurrency
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.viewmodel.data.BalanceAccount
import kotlin.math.roundToLong

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BalanceSheetView(
    accounts: List<BalanceAccount>,
    onClose: () -> Unit = {}
) {
    val (assets, liabilities) = accounts
        .groupBy { it.type }
        .mapValues { it.value.sumOf { it.equivalentCurrentBalance } to it.value }
        .entries
        .partition { it.key.isAsset }
    val totalAssets = assets.sumOf { it.value.first }
    val totalLiabilities = liabilities.sumOf { it.value.first }
    Column {
        TopAppBar(
            title = { Text(text = stringResource(R.string.balance_sheet)) },
            navigationIcon = {
                IconButton(onClick = onClose) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.Close,
                        contentDescription = "Close"
                    )
                }
            },
            actions = {
                var expanded by remember { mutableStateOf(false) }
                IconButton(onClick = { expanded = true }) {
                    androidx.compose.material3.Icon(
                        Icons.Filled.MoreVert,
                        contentDescription = "Options"
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Item 1") },
                        onClick = {
                            // Handle item 1 click
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Item 2") },
                        onClick = {
                            // Handle item 2 click
                            expanded = false
                        }
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors()
        )

        LazyColumn(modifier = Modifier
            .weight(1f)
            .padding(horizontal = dimensionResource(R.dimen.padding_main_screen))) {
            item {
                BalanceSheetSectionHeaderView(
                    name = "Assets",
                    total = totalAssets
                )
            }

            assets.forEach { (type, group) ->
                accountTypeSection(type = type, group)
            }

            item {

                BalanceSheetSectionHeaderView(
                    name = "Liabilities",
                    total = totalLiabilities
                )
            }
            liabilities.forEach { (type, group) ->
                accountTypeSection(type = type, group)
            }
            item {
                NetWorthView(totalAssets + totalLiabilities)
            }

        }
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
        ColoredAmountText(total, homeCurrency)
    }
}

fun LazyListScope.accountTypeSection(
    type: AccountType,
    group: Pair<Long, List<BalanceAccount>>,
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
                text = type.name, // Display the AccountType name
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            ColoredAmountText(total, homeCurrency)
        }
    }
    accounts.forEach { account ->
        item {
            BalanceAccountItemView(account = account)
        }
    }
    item {
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    }
}

@Composable
fun BalanceAccountItemView(account: BalanceAccount) {
    val homeCurrency = LocalHomeCurrency.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
    ) {
        Text(
            text = account.label,
            modifier = Modifier.weight(1f)
        )
        ColoredAmountText(account.equivalentCurrentBalance, homeCurrency)
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
            text = "Net Worth",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f)
        )
        ColoredAmountText(netWorth, homeCurrency)
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
                label = "EUR Cash",
                type = AccountType.CASH,
                currentBalance = 200000, // €2,000.00
                currency = "EUR",
                equivalentCurrentBalance = (200000 * 0.92).roundToLong()
            ),
            BalanceAccount(
                label = "JPY Account",
                type = AccountType.BANK,
                currentBalance = 5000000, // ¥50,000.00
                currency = "JPY",
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
                currency = "GBP",
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

