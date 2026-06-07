package org.totschnig.myexpenses.compose.accounts

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ColorCircle
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.util.ColorUtils
import org.totschnig.myexpenses.viewmodel.data.Currency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortfolioSetupDialog(
    onDismiss: () -> Unit,
    onConfirm: (label: String, currency: String, color: Int) -> Unit,
    availableCurrencies: List<Currency>
) {
    var label by remember { mutableStateOf("") }
    var selectedCurrency by remember { mutableStateOf(availableCurrencies.firstOrNull()) }
    var selectedColor by remember { mutableIntStateOf(Account.DEFAULT_COLOR) }
    var showColorPicker by remember { mutableStateOf(false) }

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
                    text = stringResource(R.string.menu_create_account) + " (Portfolio)",
                    style = MaterialTheme.typography.headlineSmall
                )

                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it },
                    label = { Text(stringResource(R.string.label)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Currency Selector
                var expanded by remember { mutableStateOf(false) }
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = !expanded }
                ) {
                    OutlinedTextField(
                        value = selectedCurrency?.toString() ?: "",
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
                                text = { Text(currency.toString()) },
                                onClick = {
                                    selectedCurrency = currency
                                    expanded = false
                                }
                            )
                        }
                    }
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

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(android.R.string.cancel))
                    }
                    Button(
                        onClick = { onConfirm(label, selectedCurrency?.code ?: "", selectedColor) },
                        enabled = label.isNotBlank() && selectedCurrency != null
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
    onColorSelected: (Int) -> Unit
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
