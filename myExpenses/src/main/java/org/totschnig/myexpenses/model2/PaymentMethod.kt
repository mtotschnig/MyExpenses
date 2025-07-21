package org.totschnig.myexpenses.model2

import android.content.Context
import androidx.annotation.IntDef
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.PreDefinedPaymentMethod

@IntDef(PAYMENT_METHOD_EXPENSE, PAYMENT_METHOD_NEUTRAL, PAYMENT_METHOD_INCOME)
@Retention(AnnotationRetention.SOURCE)
annotation class PaymentMethodType

const val PAYMENT_METHOD_EXPENSE = -1
const val PAYMENT_METHOD_NEUTRAL = 0
const val PAYMENT_METHOD_INCOME = 1


data class PaymentMethod(
    val id: Long = 0L,
    val label: String,
    val icon: String? = null,
    @PaymentMethodType val type: Int = PAYMENT_METHOD_NEUTRAL,
    val isNumbered: Boolean = false,
    val preDefinedPaymentMethod: PreDefinedPaymentMethod? = null,
    val accountTypes: List<Long>
)  {

    fun isValidForAccountType(accountType: AccountType) =
        accountTypes.contains(accountType.id)

    fun label(context: Context) = preDefinedPaymentMethod?.let { context.getString(it.resId) } ?: label
}