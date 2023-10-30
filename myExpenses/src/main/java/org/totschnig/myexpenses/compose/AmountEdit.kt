package org.totschnig.myexpenses.compose

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
                input.all { it.isDigit() } &&
                decimalSeparatorCount <= (if (fractionDigits == 0) 0 else 1) &&
                (decimalSeparatorCount == 0 || input.substringAfter(decimalSeparator).length <= fractionDigits) &&
                input.lastIndexOf('-') <= 0
            ) {
                text = input
                if (input.isEmpty()) {
                    onValueChange(BigDecimal.ZERO)
                } else {
                    Utils.validateNumber(numberFormat, input)?.let { onValueChange(it) }
                }
            }
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError
    )
}
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DenseTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions,
    isError: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val colors: TextFieldColors = OutlinedTextFieldDefaults.colors()
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        interactionSource = interactionSource,
        enabled = true,
        singleLine = true,
        keyboardOptions = keyboardOptions,
        cursorBrush = SolidColor(MaterialTheme.colorScheme.error),
        textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSurface),
    ) {
        OutlinedTextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = it,
            enabled = true,
            singleLine = true,
            visualTransformation = VisualTransformation.None,
            interactionSource = interactionSource,
            isError = isError,
            colors = colors,
            contentPadding = TextFieldDefaults.contentPaddingWithoutLabel(
                start = 8.dp,
                end = 8.dp,
                bottom = 4.dp,
            ),
            container = {
                OutlinedTextFieldDefaults.ContainerBox(true, isError, interactionSource, colors)
            },
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
