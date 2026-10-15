package org.totschnig.myexpenses.compose.accounts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ColorCircle
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.util.ColorUtils
import org.totschnig.myexpenses.util.calculateRawExchangeRate
import org.totschnig.myexpenses.util.calculateRealExchangeRate
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import java.math.BigDecimal

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, currency: String, color: Int, exchangeRate: Double, dynamicExchangeRates: Boolean, type: AccountType) -> Unit,
    onDeleteAsset: (FullAccount) -> Unit = {},
    availableCurrencies: List<CurrencyUnit>,
    availableAccountTypes: List<AccountType>,
    homeCurrency: CurrencyUnit,
    initialPortfolio: FullAccount? = null,
) {
    var label by remember { mutableStateOf(initialPortfolio?.label ?: "") }
    var selectedCurrencyState by remember { mutableStateOf(initialPortfolio?.currencyUnit ?: homeCurrency) }
    var selectedAccountType by remember {
        mutableStateOf(
            initialPortfolio?.type ?: availableAccountTypes.find { it.name == AccountType.INVESTMENT.name }
            ?: availableAccountTypes.firstOrNull()
            ?: AccountType.INVESTMENT
        )
    }
    var selectedColor by remember { mutableIntStateOf(initialPortfolio?.color ?: Account.DEFAULT_COLOR) }
    var showColorPicker by remember { mutableStateOf(false) }

    var dynamicExchangeRates by remember { mutableStateOf(initialPortfolio?.dynamic ?: false) }
    var exchangeRate by remember {
        mutableStateOf(
            initialPortfolio?.initialExchangeRate?.let {
                calculateRealExchangeRate(it, initialPortfolio.currencyUnit, homeCurrency).toPlainString()
            } ?: "1"
        )
    }

    val isExchangeRateValid = remember(exchangeRate) {
        try {
            BigDecimal(exchangeRate) > BigDecimal.ZERO
        } catch (_: Exception) {
            false
        }
    }

    val context = LocalContext.current
    val scrollState = rememberScrollState()
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                val title = if (initialPortfolio == null) {
                    stringResource(R.string.menu_create_portfolio)
                } else {
                    stringResource(R.string.menu_edit_account)
                }
                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Account Type Selector
                var typeExpanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = typeExpanded,
                    onExpandedChange = { typeExpanded = !typeExpanded }
                ) {
                    OutlinedTextField(
                        value = selectedAccountType.title(context),
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.account_types)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = typeExpanded,
                        onDismissRequest = { typeExpanded = false }
                    ) {
                        availableAccountTypes.forEach { type ->
                            DropdownMenuItem(
                                text = { Text(type.title(context)) },
                                onClick = {
                                    selectedAccountType = type
                                    typeExpanded = false
                                }
                            )
                        }
                    }
                }

                // Currency Selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCurrencyState.description,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.currency)) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false }
                    ) {
                        availableCurrencies.forEach { currency ->
                            DropdownMenuItem(
                                text = { Text(currency.description) },
                                onClick = {
                                    selectedCurrencyState = currency
                                    expanded = false
                                }
                            )
                        }
                    }
                }

                val isFx = selectedCurrencyState.code != homeCurrency.code
                if (isFx) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.dynamic_exchange_rate),
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Switch(
                            checked = dynamicExchangeRates,
                            onCheckedChange = { dynamicExchangeRates = it }
                        )
                    }

                    OutlinedTextField(
                        value = exchangeRate,
                        onValueChange = { exchangeRate = it },
                        label = { Text(stringResource(R.string.exchange_rate)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        enabled = !dynamicExchangeRates,
                        isError = !isExchangeRateValid,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        prefix = { Text("1 ${selectedCurrencyState.code} = ") },
                        suffix = { Text(" ${homeCurrency.code}") }
                    )
                }

                // Color Selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.clickable { showColorPicker = true }
                ) {
                    ColorCircle(modifier = Modifier.size(32.dp), color = selectedColor)
                    Text(stringResource(R.string.color), style = MaterialTheme.typography.bodyLarge)
                }

                if (showColorPicker) {
                    ColorPickerDialog(
                        onDismiss = { showColorPicker = false },
                        onColorSelected = {
                            selectedColor = it
                            showColorPicker = false
                        }
                    )
                }

                if (initialPortfolio != null && initialPortfolio.children.isNotEmpty()) {
                    HorizontalDivider()
                    Text(
                        text = stringResource(R.string.balance_sheet_section_assets),
                        style = MaterialTheme.typography.titleMedium
                    )
                    initialPortfolio.children.forEach { asset ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = asset.label, modifier = Modifier.weight(1f))
                            IconButton(onClick = { onDeleteAsset(asset) }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = stringResource(R.string.menu_delete)
                                )
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(
                        onClick = {
                            val rate = try {
                                BigDecimal(exchangeRate)
                            } catch (_: Exception) {
                                BigDecimal.ONE
                            }
                            onConfirm(
                                label,
                                selectedCurrencyState.code,
                                selectedColor,
                                calculateRawExchangeRate(rate, selectedCurrencyState, homeCurrency),
                                dynamicExchangeRates,
                                selectedAccountType
                            )
                        },
                        enabled = label.isNotBlank() && (!isFx || dynamicExchangeRates || isExchangeRateValid)
                    ) {
                        Text(stringResource(android.R.string.ok))
                    }
                }
            }
        }
    }
}

@Composable
fun ColorPickerDialog(
    onDismiss: () -> Unit,
    onColorSelected: (Int) -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    stringResource(R.string.color),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(48.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.heightIn(max = 300.dp)
                ) {
                    items(ColorUtils.MAIN_COLORS.toList()) { color ->
                        ColorCircle(
                            modifier = Modifier
                                .size(48.dp)
                                .clickable { onColorSelected(color) },
                            color = color
                        )
                    }
                }
            }
        }
    }
}
