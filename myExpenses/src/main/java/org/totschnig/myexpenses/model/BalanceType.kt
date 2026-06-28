package org.totschnig.myexpenses.model

import org.totschnig.myexpenses.util.TextUtils.joinEnum

enum class BalanceType {
    CURRENT,
    TOTAL,
    CLEARED,
    RECONCILED,
    DELTA;

    companion object {
        val JOIN: String = joinEnum<BalanceType>()
    }
}