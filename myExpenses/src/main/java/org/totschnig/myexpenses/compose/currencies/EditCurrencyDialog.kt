package org.totschnig.myexpenses.compose.currencies

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.google.common.math.IntMath
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.CommodityType
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.util.Utils
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditCurrencyDialog(
    currency: CurrencyUnit?,
    onDismiss: () -> Unit,
    onConfirm: (
        code: String,
        symbol: String,
        fractionDigits: Int,
        label: String?,
        commodityType: CommodityType,
        withUpdate: Boolean
    ) -> Unit,
    isCurrencyUsed: suspend (String) -> Boolean,
    initialCode: String = "",
    isSaving: Boolean = false,
    errorMessage: Int? = null,
    defaultType: CommodityType = CommodityType.FIAT,
    allowedTypes: List<CommodityType> = CommodityType.entries,
) {
    val isEdit = currency != null
    val isKnownCurrency = isEdit && Utils.isKnownCurrency(currency.code)

    var symbol by rememberSaveable { mutableStateOf(currency?.symbol ?: "") }
    var code by rememberSaveable { mutableStateOf(currency?.code ?: initialCode) }
    var label by rememberSaveable { mutableStateOf(currency?.description ?: "") }
    var commodityType by rememberSaveable {
        mutableStateOf(
            if (currency?.commodityType in allowedTypes) currency!!.commodityType
            else if (defaultType in allowedTypes) defaultType
            else allowedTypes.firstOrNull() ?: CommodityType.FIAT
        )
    }
    var fractionDigitsStr by rememberSaveable {
        mutableStateOf(
            currency?.fractionDigits?.toString() ?: when (commodityType) {
                CommodityType.CRYPTO -> "8"
                else -> "2"
            }
        )
    }
    var withUpdate by rememberSaveable { mutableStateOf(false) }

    val initialFractionDigits = currency?.fractionDigits ?: 2
    val currentFractionDigits = fractionDigitsStr.toIntOrNull() ?: -1
    val fractionDigitsUpdate =
        isEdit && currentFractionDigits != -1 && currentFractionDigits != initialFractionDigits

    var isCurrencyUsedResult by rememberSaveable { mutableStateOf(isEdit) }

    LaunchedEffect(currency?.code) {
        currency?.code?.let {
            isCurrencyUsedResult = isCurrencyUsed(it)
        }
    }

    val warningMessage = if (fractionDigitsUpdate && isCurrencyUsedResult) {
        val delta = initialFractionDigits - currentFractionDigits
        val part1 = stringResource(R.string.warning_change_fraction_digits_1)
        val part2 = stringResource(
            if (delta > 0) R.string.warning_change_fraction_digits_2_multiplied else R.string.warning_change_fraction_digits_2_divided,
            IntMath.pow(10, abs(delta))
        )
        val part3 = if (delta > 0) " " + stringResource(R.string.warning_change_fraction_digits_3) else ""
        "$part1 $part2$part3"
    } else null

    var localErrorMessage by rememberSaveable { mutableStateOf<Int?>(null) }
    LaunchedEffect(errorMessage) {
        localErrorMessage = errorMessage
    }

    val isValid = (isEdit || (code.isNotEmpty() && label.isNotEmpty())) && currentFractionDigits in 0..8

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                when {
                    isKnownCurrency -> String.format(
                        Locale.ROOT,
                        "%s (%s)",
                        currency.description,
                        currency.code
                    )

                    isEdit -> currency.description
                    else -> stringResource(R.string.dialog_title_new_currency)
                }
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth()
            ) {
                if (!isKnownCurrency && allowedTypes.size > 1) {
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        allowedTypes.forEachIndexed { index, type ->
                            SegmentedButton(
                                shape = SegmentedButtonDefaults.itemShape(
                                    index = index,
                                    count = allowedTypes.size
                                ),
                                onClick = { commodityType = type },
                                selected = commodityType == type,
                                label = { Text(stringResource(type.labelSingular)) }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = label,
                    onValueChange = { label = it; localErrorMessage = null },
                    label = { Text(stringResource(R.string.label)) },
                    singleLine = true,
                    enabled = !isKnownCurrency,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.uppercase(); localErrorMessage = null },
                    label = { Text(stringResource(R.string.currency_code)) },
                    enabled = !isKnownCurrency,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = symbol,
                    onValueChange = { symbol = it },
                    label = { Text(stringResource(R.string.currency_symbol)) },
                    placeholder = { Text(code) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = fractionDigitsStr,
                    onValueChange = { if (it.length <= 1) fractionDigitsStr = it },
                    label = { Text(stringResource(R.string.number_of_fraction_digits)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                warningMessage?.let { text ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = withUpdate,
                            onCheckedChange = { withUpdate = it }
                        )
                        Text(
                            stringResource(R.string.warning_change_fraction_digits_checkbox_label),
                            modifier = Modifier.clickable { withUpdate = !withUpdate }
                        )
                    }
                    Text(
                        text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                localErrorMessage?.let {
                    Text(
                        stringResource(it),
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = isValid && !isSaving, onClick = {
                onConfirm(
                    code,
                    symbol.takeIf { it.isNotEmpty() } ?: code,
                    currentFractionDigits,
                    label.takeUnless { isKnownCurrency },
                    commodityType,
                    withUpdate
                )
            }) { Text(stringResource(android.R.string.ok)) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        }
    )
}
