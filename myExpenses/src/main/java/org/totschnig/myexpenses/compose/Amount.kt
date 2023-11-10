package org.totschnig.myexpenses.compose

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.db2.FLAG_TRANSFER
import org.totschnig.myexpenses.model.CurrencyUnit
import org.totschnig.myexpenses.model.Money
import kotlin.math.sign

fun Modifier.amountBorder(color: Color) = this
    .border(
        border = BorderStroke(1.dp, color),
        shape = RoundedCornerShape(16.dp),
    )
    .padding(8.dp)

@Composable
fun AmountText(
    amount: Long,
    currency: CurrencyUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    textDecoration: TextDecoration? = null,
    color: Color = Color.Unspecified,
    prefix: String = "",
    postfix: String = ""
) {
    val money = Money(currency, amount)
    Text(
        modifier = modifier.amountSemantics(amount),
        fontWeight = fontWeight,
        textAlign = textAlign,
        textDecoration = textDecoration,
        color = color,
        text = prefix + LocalCurrencyFormatter.current.formatCurrency(money.amountMajor, money.currencyUnit) + postfix
    )
}

@Composable
fun ColoredAmountText(
    amount: Long,
    currency: CurrencyUnit,
    modifier: Modifier = Modifier,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    withBorder: Boolean = false,
    prefix: String = "",
    postfix: String = ""
) {
    ColoredAmountText(
        money = Money(currency, amount),
        modifier = modifier,
        fontWeight = fontWeight,
        textAlign = textAlign,
        withBorder = withBorder,
        prefix = prefix,
        postfix = postfix
    )
}

@Composable
fun ColoredAmountText(
    money: Money,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    withBorder: Boolean = false,
    prefix: String = "",
    postfix: String = "",
    type: Byte? = when(money.amountMinor.sign) {
        1 -> FLAG_INCOME
        -1 -> FLAG_EXPENSE
        else -> null
    }
) {
    val color = when(type) {
        FLAG_INCOME-> LocalColors.current.income
        FLAG_EXPENSE -> LocalColors.current.expense
        FLAG_TRANSFER -> LocalColors.current.transfer
        else -> Color.Unspecified
    }
    Text(
        modifier = modifier
            .conditional(withBorder) { amountBorder(color) }
            .amountSemantics(money.amountMinor),
        fontWeight = fontWeight,
        textAlign = textAlign,
        style = style,
        text = prefix + LocalCurrencyFormatter.current.formatCurrency(money.amountMajor, money.currencyUnit) + postfix,
        color = color
    )
}

@Preview
@Composable
fun AmountPreview() {
    ColoredAmountText(money = Money(CurrencyUnit.DebugInstance, 8000), withBorder = true)
}