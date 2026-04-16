package org.totschnig.myexpenses.compose.main

import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.model.BalanceType
import org.totschnig.myexpenses.viewmodel.data.AggregateAccount
import org.totschnig.myexpenses.viewmodel.data.BaseAccount
import org.totschnig.myexpenses.viewmodel.data.FullAccount
import kotlin.math.absoluteValue
import kotlin.math.sign

val BaseAccount.validatedBalanceType
    get() = balanceType.takeIf {
        when (it) {
            BalanceType.CURRENT -> true
            BalanceType.DELTA -> (this as? FullAccount)?.criterion != null
            BalanceType.TOTAL -> (total ?: equivalentTotal) != null
            BalanceType.CLEARED, BalanceType.RECONCILED ->  this is FullAccount && type.supportsReconciliation
        }
    } ?: BalanceType.CURRENT

val BaseAccount.balanceForType: Long
    get() {
        val type = validatedBalanceType
        return when (this) {
            is FullAccount -> when (type) {
                BalanceType.CURRENT -> currentBalance
                BalanceType.TOTAL -> total!!
                BalanceType.CLEARED -> clearedTotal
                BalanceType.RECONCILED -> reconciledTotal
                BalanceType.DELTA -> delta
            }

            is AggregateAccount -> when (type) {
                BalanceType.CURRENT -> currentBalance ?: equivalentCurrentBalance
                BalanceType.TOTAL -> total ?: equivalentTotal
                else -> throw IllegalStateException("Unsupported balance type for aggregate account: $type")
            }
        }
    }

fun BaseAccount.geBalanceContentDescription(type: BalanceType): Int = when(type) {
    BalanceType.CURRENT -> R.string.current_balance
    BalanceType.TOTAL -> R.string.menu_aggregates
    BalanceType.CLEARED -> R.string.total_cleared
    BalanceType.RECONCILED -> R.string.total_reconciled
    BalanceType.DELTA -> (this as FullAccount).deltaLabel
}

val FullAccount.delta: Long
    get() = currentBalance - criterion!!

val FullAccount.deltaLabel: Int
    get() {
        require(criterion != null)
        val isSavingGoal = criterion.sign > 0
        val hasReached = criterion.sign == currentBalance.sign &&
                currentBalance.absoluteValue >= criterion.absoluteValue
        return when {
            hasReached -> R.string.overage
            isSavingGoal -> R.string.saving_goal_short_fall
            else -> R.string.credit_limit_available_credit
        }
    }