package org.totschnig.myexpenses.compose

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.Utils
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

@Composable
fun AmountEdit(
    value: BigDecimal,
    onValueChange: (BigDecimal) -> Unit,
    fractionDigits: Int = 2
) {
    val digits = stringResource(id = R.string.amount_digits).toCharArray() + '-'
    val decimalSeparator = remember { Utils.getDefaultDecimalSeparator() }
    // TODO we should take into account the arab separator as well
    val otherSeparator = remember { if (decimalSeparator == '.') ',' else '.' }

    val numberFormat = remember {
        val symbols = DecimalFormatSymbols()
        symbols.decimalSeparator = decimalSeparator
        var pattern = "#0"
        if (fractionDigits > 0) {
            pattern += "." + String(CharArray(fractionDigits)).replace("\u0000", "#")
        }
        DecimalFormat(pattern, symbols).also {
            it.isGroupingUsed = false
        }
    }

    var text by rememberSaveable {
        mutableStateOf(numberFormat.format(value))
    }

    TextField(
        value = text,
        onValueChange = {
            val input = it.replace(otherSeparator, decimalSeparator)
            val decimalSeparatorCount = input.count { it == decimalSeparator }
            if (
                it.all { digits.indexOf(it) > -1 } &&
                decimalSeparatorCount <= (if (fractionDigits == 0) 0 else 1) &&
                (decimalSeparatorCount == 0 || input.substringAfter(decimalSeparator).length <= fractionDigits) &&
                input.lastIndexOf('-') <= 0
            ) {
                text = it
                Utils.validateNumber(numberFormat, input)?.let { it1 -> onValueChange(it1) }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
}

@Preview
@Composable
fun AmountEditPreview() {
    var value by remember {
        mutableStateOf(BigDecimal.TEN)
    }
    AmountEdit(
        value = value,
        onValueChange = { value = it },
        fractionDigits = 2
    )
}
