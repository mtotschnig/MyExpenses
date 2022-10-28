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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.util.formatMoney

fun Modifier.amountBorder(color: Color) = this
    .border(
        border = BorderStroke(
            ButtonDefaults.OutlinedBorderSize,
            color
        ),
        shape = RoundedCornerShape(16.dp),
    )
    .padding(8.dp)

@Composable
fun ColoredAmountText(
    amount: Long,
    currency: CurrencyUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    withBorder: Boolean = false,
    prefix: String = "",
    postFix: String = ""
) {
    ColoredAmountText(money = Money(currency, amount), modifier, fontWeight, textAlign, withBorder, prefix, postFix)
}

@Composable
fun ColoredAmountText(
    money: Money,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    withBorder: Boolean = false,
    prefix: String = "",
    postFix: String = ""
) {
    val color = when {
        money.amountMinor > 0 -> LocalColors.current.income
        money.amountMinor < 0 -> LocalColors.current.expense
        else -> Color.Unspecified
    }
    Text(
        modifier = if (withBorder) modifier.amountBorder(color) else modifier,
        fontWeight = fontWeight,
        textAlign = textAlign,
        text = prefix + LocalCurrencyFormatter.current.formatMoney(money) + postFix,
        color = color
    )
}

@Preview
@Composable
fun AmountPreview() {
    ColoredAmountText(money = Money(CurrencyUnit.DebugInstance, 8000), withBorder = true)
}