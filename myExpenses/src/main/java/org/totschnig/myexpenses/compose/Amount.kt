package org.totschnig.myexpenses.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.model.CurrencyUnit

@Composable
fun ColoredAmountText(
    amount: Long,
    currency: CurrencyUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    withBorder: Boolean = false
) {
    val color = when {
        amount > 0 -> LocalColors.current.income
        amount < 0 -> LocalColors.current.expense
        else -> Color.Unspecified
    }
    Text(
        modifier = modifier.then(
            if (withBorder)
                Modifier
                    .border(
                        border = BorderStroke(
                            ButtonDefaults.OutlinedBorderSize,
                            color
                        ),
                        shape = RoundedCornerShape(16.dp),
                    )
                    .padding(8.dp) else Modifier
        ),
        fontWeight = fontWeight,
        textAlign = textAlign,
        text = LocalAmountFormatter.current(amount, currency),
        color = color
    )
}