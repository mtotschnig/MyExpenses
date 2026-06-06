package org.totschnig.myexpenses.compose.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AmountEdit
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.viewmodel.data.TradeIntent
import org.totschnig.myexpenses.viewmodel.data.TradeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(
    onDismiss: () -> Unit,
    onSave: (TradeIntent) -> Unit,
    reportingCurrency: CurrencyUnit,
    availableAssets: List<CurrencyUnit>,
    availableAccounts: List<Pair<Long, String>> // ID to Label
) {
    var intent by remember { mutableStateOf(TradeIntent(feeAsset = reportingCurrency)) }

    val principal = remember(intent.quantity, intent.price) {
        intent.quantity.multiply(intent.price)
    }

    val total = remember(intent.type, principal, intent.fee) {
        if (intent.type == TradeType.BUY) principal.add(intent.fee) else principal.subtract(intent.fee)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(if (intent.type == TradeType.BUY) R.string.trade_buy else if (intent.type == TradeType.SELL) R.string.trade_sell else R.string.trade_swap)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(intent) }) {
                        Icon(Icons.Default.Done, contentDescription = stringResource(R.string.menu_save))
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trade Type Toggle
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TradeType.entries.forEachIndexed { index, type ->
                    SegmentedButton(
                        selected = intent.type == type,
                        onClick = { intent = intent.copy(type = type) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = TradeType.entries.size)
                    ) {
                        Text(stringResource(when(type) {
                            TradeType.BUY -> R.string.trade_buy
                            TradeType.SELL -> R.string.trade_sell
                            TradeType.SWAP -> R.string.trade_swap
                        }))
                    }
                }
            }

            // Asset Selection
            AssetSelector(
                label = stringResource(R.string.trade_target_asset),
                selectedAsset = intent.targetAsset,
                assets = availableAssets,
                onAssetSelected = { intent = intent.copy(targetAsset = it) }
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.trade_quantity), style = MaterialTheme.typography.labelMedium)
                    AmountEdit(
                        value = intent.quantity,
                        onValueChange = { intent = intent.copy(quantity = it) },
                        fractionDigits = intent.targetAsset?.fractionDigits ?: 2
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(stringResource(R.string.trade_price), style = MaterialTheme.typography.labelMedium)
                    AmountEdit(
                        value = intent.price,
                        onValueChange = { intent = intent.copy(price = it) },
                        fractionDigits = reportingCurrency.fractionDigits
                    )
                }
            }

            // Principal Display
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.trade_principal), style = MaterialTheme.typography.bodyMedium)
                Text(principal.toString() + " " + reportingCurrency.symbol, style = MaterialTheme.typography.bodyLarge)
            }

            // Fee
            Column {
                Text(stringResource(R.string.trade_fee), style = MaterialTheme.typography.labelMedium)
                AmountEdit(
                    value = intent.fee,
                    onValueChange = { intent = intent.copy(fee = it) },
                    fractionDigits = reportingCurrency.fractionDigits
                )
            }

            // Total Display
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(stringResource(R.string.trade_total_outlay), style = MaterialTheme.typography.bodyMedium)
                Text(total.toString() + " " + reportingCurrency.symbol, style = MaterialTheme.typography.bodyLarge)
            }

            if (intent.type == TradeType.SWAP) {
                AssetSelector(
                    label = stringResource(R.string.trade_swap), // Use a proper label if needed
                    selectedAsset = intent.sourceAsset,
                    assets = availableAssets,
                    onAssetSelected = { intent = intent.copy(sourceAsset = it) }
                )
            } else {
                // Funding Account Selection
                AccountSelector(
                    label = stringResource(R.string.trade_funding_account),
                    selectedAccountId = intent.fundingAccountId,
                    accounts = availableAccounts,
                    onAccountSelected = { intent = intent.copy(fundingAccountId = it) }
                )
            }

            // Comment
            OutlinedTextField(
                value = intent.comment,
                onValueChange = { intent = intent.copy(comment = it) },
                label = { Text(stringResource(R.string.notes)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssetSelector(
    label: String,
    selectedAsset: CurrencyUnit?,
    assets: List<CurrencyUnit>,
    onAssetSelected: (CurrencyUnit) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedAsset?.code ?: "",
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            assets.forEach { asset ->
                DropdownMenuItem(
                    text = { Text("${asset.code} (${asset.symbol})") },
                    onClick = {
                        onAssetSelected(asset)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSelector(
    label: String,
    selectedAccountId: Long?,
    accounts: List<Pair<Long, String>>,
    onAccountSelected: (Long) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedAccountLabel = accounts.find { it.first == selectedAccountId }?.second ?: ""
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedAccountLabel,
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable).fillMaxWidth()
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            accounts.forEach { account ->
                DropdownMenuItem(
                    text = { Text(account.second) },
                    onClick = {
                        onAccountSelected(account.first)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun TradeScreenPreview() {
    TradeScreen(
        onDismiss = {},
        onSave = {},
        reportingCurrency = CurrencyUnit("EUR", "€", 2),
        availableAssets = listOf(
            CurrencyUnit("AAPL", "AAPL", 4),
            CurrencyUnit("BTC", "₿", 8)
        ),
        availableAccounts = listOf(
            1L to "Cash",
            2L to "Bank Account"
        )
    )
}
