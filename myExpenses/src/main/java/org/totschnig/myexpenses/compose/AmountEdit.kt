package org.totschnig.myexpenses.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.TextFieldDefaults
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.util.Utils
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols

@Composable
fun AmountEdit(
    value: BigDecimal,
    onValueChange: (BigDecimal) -> Unit,
    fractionDigits: Int = 2,
    isError: Boolean
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

    DenseTextField(
        value = text,
        onValueChange = { newValue ->
            val input = newValue.replace(otherSeparator, decimalSeparator)
            val decimalSeparatorCount = input.count { it == decimalSeparator }
            if (
                input.all { digits.indexOf(it) > -1 } &&
                decimalSeparatorCount <= (if (fractionDigits == 0) 0 else 1) &&
                (decimalSeparatorCount == 0 || input.substringAfter(decimalSeparator).length <= fractionDigits) &&
                input.lastIndexOf('-') <= 0
            ) {
                text = input
                if (input.isEmpty()) {
                    onValueChange(BigDecimal.ZERO)
                } else {
                    Utils.validateNumber(numberFormat, input)?.let { it -> onValueChange(it) }
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError
    )
}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun DenseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions,
    isError: Boolean
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors = TextFieldDefaults.outlinedTextFieldColors()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        interactionSource = interactionSource,
        enabled = true,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(colors.cursorColor(isError).value)
    ) {
        TextFieldDefaults.OutlinedTextFieldDecorationBox(
            value = value,
            visualTransformation = VisualTransformation.None,
            innerTextField = it,
            singleLine = true,
            enabled = true,
            interactionSource = interactionSource,
            contentPadding = TextFieldDefaults.textFieldWithoutLabelPadding(
                start = 8.dp, end = 8.dp, bottom = 4.dp
            ),
            isError = isError
        )
    }
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
        fractionDigits = 2,
        isError = false
    )
}
