package org.totschnig.myexpenses.compose

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign

@Composable
fun ColoredAmountText(
    amount: Long,
    currency: String,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null
) {
    Text(
        modifier = modifier,
        fontWeight = fontWeight,
        textAlign = textAlign,
        text = LocalAmountFormatter.current(amount, currency),
        color = when {
            amount > 0 -> LocalColors.current.income
            amount < 0 -> LocalColors.current.expense
            else -> Color.Unspecified
        }
    )
}