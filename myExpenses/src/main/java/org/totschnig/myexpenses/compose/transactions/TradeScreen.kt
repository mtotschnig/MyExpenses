package org.totschnig.myexpenses.compose.transactions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AmountEdit
import org.totschnig.myexpenses.compose.LocalCurrencyContext
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.viewmodel.data.Currency
import org.totschnig.myexpenses.viewmodel.data.TradeIntent
import org.totschnig.myexpenses.viewmodel.data.TradeType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TradeScreen(
    onDismiss: () -> Unit,
    onSave: (TradeIntent) -> Unit,
    reportingCurrency: CurrencyUnit,
    assets: List<Currency>,
    availableAccounts: List<Pair<Long, String>>, // ID to Label
    initialAction: Action,
    onCreateAsset: (Currency) -> Unit = {},
) {

    val currencyContext = LocalCurrencyContext.current

    var showAddAsset by remember { mutableStateOf<Pair<String, CommodityType>?>(null) }

    showAddAsset?.let { (code, type) ->
        AddAssetDialog(
            initialCode = code,
            commodityType = type,
            onDismiss = { showAddAsset = null },
            onConfirm = { newAsset ->
                onCreateAsset(newAsset)
                showAddAsset = null
            }
        )
    }

    var intent by remember {
        mutableStateOf(
            TradeIntent(
                type = if (initialAction == Action.Sell) TradeType.SELL else TradeType.BUY,
                feeAsset = reportingCurrency
            )
        )
    }

    var selectedAsset by remember { mutableStateOf<Currency?>(null) }

    val principal = remember(intent.quantity, intent.price) {
        intent.quantity.multiply(intent.price)
    }

    val total = remember(intent.type, principal, intent.fee) {
        if (intent.type == TradeType.BUY) principal.add(intent.fee) else principal.subtract(intent.fee)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        TradeType.entries.forEachIndexed { index, type ->
                            SegmentedButton(
                                selected = intent.type == type,
                                onClick = { intent = intent.copy(type = type) },
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = TradeType.entries.size
                                )
                            ) {
                                Text(
                                    stringResource(
                                        when (type) {
                                            TradeType.BUY -> R.string.trade_buy
                                            TradeType.SELL -> R.string.trade_sell
                                        }
                                    )
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                    }
                },
                actions = {
                    IconButton(onClick = { onSave(intent) }) {
                        Icon(
                            Icons.Default.Done,
                            contentDescription = stringResource(R.string.menu_save)
                        )
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
            // Asset Selection
            AssetSelector(
                label = stringResource(R.string.trade_target_asset),
                selectedAsset = intent.targetAsset,
                assets = assets,
                onAssetSelected = { selectedAsset = it },
                onCreateAsset = { showAddAsset = it }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.trade_quantity),
                        style = MaterialTheme.typography.labelMedium
                    )
                    AmountEdit(
                        value = intent.quantity,
                        onValueChange = { intent = intent.copy(quantity = it) },
                        fractionDigits = selectedAsset?.let { currencyContext[it.code] }?.fractionDigits
                            ?: 2,
                        enabled = selectedAsset != null
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.trade_price),
                        style = MaterialTheme.typography.labelMedium
                    )
                    AmountEdit(
                        value = intent.price,
                        onValueChange = { intent = intent.copy(price = it) },
                        fractionDigits = reportingCurrency.fractionDigits
                    )
                }
            }

            // Principal Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.trade_principal),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    principal.toString() + " " + reportingCurrency.symbol,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Fee
            Column {
                Text(
                    stringResource(R.string.trade_fee),
                    style = MaterialTheme.typography.labelMedium
                )
                AmountEdit(
                    value = intent.fee,
                    onValueChange = { intent = intent.copy(fee = it) },
                    fractionDigits = reportingCurrency.fractionDigits
                )
            }

            // Total Display
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    stringResource(R.string.trade_total_outlay),
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    total.toString() + " " + reportingCurrency.symbol,
                    style = MaterialTheme.typography.bodyLarge
                )
            }

            // Funding Account Selection
            AccountSelector(
                label = stringResource(R.string.trade_funding_account),
                selectedAccountId = intent.fundingAccountId,
                accounts = availableAccounts,
                onAccountSelected = { intent = intent.copy(fundingAccountId = it) }
            )

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
    selectedAsset: Currency?,
    assets: List<Currency>,
    onAssetSelected: (Currency) -> Unit,
    onCreateAsset: (Pair<String, CommodityType>) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf(selectedAsset?.code ?: "") }

    var selectedType by remember { mutableStateOf(CommodityType.SECURITY) }

    val filteredAssets = remember(searchQuery, selectedType, assets) {
        assets.filter { asset ->
            val matchesQuery = asset.code.contains(searchQuery, ignoreCase = true) ||
                    asset.symbol.contains(searchQuery, ignoreCase = true)
            val matchesType = asset.commodityType == selectedType
            matchesQuery && matchesType
        }.sortedWith(compareBy({ it.commodityType == CommodityType.FIAT }, { -it.usages }))
            .take(20)
    }

    val exactMatchExists = filteredAssets.any { it.code.equals(searchQuery, ignoreCase = true) }

    val showCreateOption =
        searchQuery.isNotBlank() && !exactMatchExists && selectedType != CommodityType.FIAT

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = {
                searchQuery = it
                expanded = true
            },
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable)
                .fillMaxWidth(),
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
        )
        if (expanded) {
            val groupedAssets = filteredAssets
                .distinctBy { it.code }
                .groupBy { it.commodityType }
                .toSortedMap()
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CommodityType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = {
                                Text(
                                    type.name.lowercase().replaceFirstChar { it.uppercase() })
                            },
                            // Optional: Small icons make it feel more premium
                            leadingIcon = if (selectedType == type) {
                                { Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp)) }
                            } else null
                        )
                    }
                }
                HorizontalDivider()
                groupedAssets.forEach { (type, assets) ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = type.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        },
                        onClick = {},
                        enabled = false
                    )
                    assets.forEach { asset ->
                        DropdownMenuItem(
                            text = { Text("${asset.displayName} (${asset.code})") },
                            onClick = {
                                searchQuery = asset.code
                                onAssetSelected(asset)
                                expanded = false
                            }
                        )
                    }

                }
                if (showCreateOption) {
                    DropdownMenuItem(
                        text = { Text("Add \"$searchQuery\" as ${selectedType.name.lowercase()}") },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null) },
                        onClick = {
                            onCreateAsset(searchQuery to selectedType)
                            expanded = false
                        }
                    )
                }
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
    onAccountSelected: (Long) -> Unit,
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
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
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

@Composable
fun AddAssetDialog(
    initialCode: String,
    commodityType: CommodityType,
    onDismiss: () -> Unit,
    onConfirm: (Currency) -> Unit,
) {
    var code by remember { mutableStateOf(initialCode) }
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("") }
    var fractionDigits by remember {
        mutableStateOf(
            when (commodityType) {
                CommodityType.CRYPTO -> 8
                else -> 0
            }
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    "Add ${commodityType.name.lowercase()}",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase() },
                    label = { Text(stringResource(R.string.currency_code)) },
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.label)) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (commodityType == CommodityType.CRYPTO) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it },
                        label = { Text(stringResource(R.string.currency_symbol)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // In the UI
                OutlinedTextField(
                    value = fractionDigits.toString(),
                    onValueChange = { fractionDigits = it.toIntOrNull() ?: 0 },
                    label = { Text(stringResource(R.string.number_of_fraction_digits)) },
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
                    Button(
                        onClick = {
                            onConfirm(
                                Currency(
                                    code = code,
                                    displayName = name,
                                    symbol = symbol,
                                    fractionDigits = fractionDigits,
                                    commodityType = commodityType
                                )
                            )
                        },
                        enabled = code.isNotBlank() && name.isNotBlank()
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}


@Preview
@Composable
fun TradeScreenPreview() {
    TradeScreen(
        onDismiss = {},
        onSave = {},
        reportingCurrency = CurrencyUnit("EUR", "€", 2),
        assets = listOf(
            Currency("AAPL", "AAPL"),
            Currency("BTC", "₿")
        ),
        availableAccounts = listOf(
            1L to "Cash",
            2L to "Bank Account"
        ),
        initialAction = Action.Buy
    )
}
