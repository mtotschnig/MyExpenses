package org.totschnig.myexpenses.util

import android.content.Context
import eltos.simpledialogfragment.form.AmountEdit
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.Money
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.TextUtils.appendCurrencySymbol
import java.math.BigDecimal
import java.util.*

fun getBackgroundForAvailable(onBudget: Boolean): Int {
    return if (onBudget) R.drawable.round_background_income else R.drawable.round_background_expense
}

fun buildAmountField(
    amount: Money, max: BigDecimal?, min: BigDecimal?,
    level: Int, context: Context
): AmountEdit? {
    val amountEdit = AmountEdit.plain(DatabaseConstants.KEY_AMOUNT)
        .label(appendCurrencySymbol(context, R.string.budget_allocated_amount, amount.currencyUnit))
        .fractionDigits(amount.currencyUnit.fractionDigits).required()
    if (amount.amountMajor.compareTo(BigDecimal.ZERO) != 0) {
        amountEdit.amount(amount.amountMajor)
    }
    if (max != null) {
        amountEdit.max(
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
        amountEdit.min(
            min,
            context.getString(
                if (level == 1) R.string.sub_budget_under_allocated_error else R.string.budget_under_allocated_error,
                min
            )
        )
    }
    return amountEdit
}