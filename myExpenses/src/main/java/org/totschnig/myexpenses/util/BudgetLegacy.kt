package org.totschnig.myexpenses.util

import android.content.Context
import eltos.simpledialogfragment.form.AmountInput
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol
import java.math.BigDecimal
import java.util.*

fun buildAmountField(
    amount: Money, max: BigDecimal?, min: BigDecimal?,
    level: Int, context: Context
): AmountInput? {
    val amountInput = AmountInput.plain(DatabaseConstants.KEY_AMOUNT)
        .label(appendCurrencySymbol(context, R.string.budget_allocated_amount, amount.currencyUnit))
        .fractionDigits(amount.currencyUnit.fractionDigits)
        .withTypeSwitch(null)
        .required()
    if (amount.amountMajor.compareTo(BigDecimal.ZERO) != 0) {
        amountInput.amount(amount.amountMajor)
    }
    if (max != null) {
        amountInput.max(
            max, String.format(
                Locale.ROOT, "%s %s",
                context.getString(
                    if (level > 1) R.string.sub_budget_exceeded_error_1_1 else R.string.budget_exceeded_error_1_1,
                    max
                ),
                context.getString(if (level > 1) R.string.sub_budget_exceeded_error_2 else R.string.budget_exceeded_error_2)
            )
        )
    }
    if (min != null) {
        amountInput.min(
            min,
            context.getString(
                if (level == 1) R.string.sub_budget_under_allocated_error else R.string.budget_under_allocated_error,
                min
            )
        )
    }
    return amountInput
}