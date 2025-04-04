package org.totschnig.myexpenses.model

import androidx.annotation.StringRes
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SORT_KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.util.TextUtils.joinEnum


enum class AccountType(val isAsset: Boolean) {
    CASH(true), BANK(true), CCARD(false), ASSET(true), LIABILITY(false);

    fun toStringResPlural() = when (this) {
        CASH -> R.string.account_type_cash_plural
        BANK -> R.string.account_type_bank_plural
        CCARD -> R.string.account_type_ccard_plural
        ASSET -> R.string.account_type_asset_plural
        LIABILITY -> R.string.account_type_liability_plural
    }

    fun toQifName() = when (this) {
        CASH -> "Cash"
        BANK -> "Bank"
        CCARD -> "CCard"
        ASSET -> "Oth A"
        LIABILITY -> "Oth L"
    }

    private val sortOrder: String
        get() = when (this) {
            CASH -> "0"
            BANK -> "1"
            CCARD -> "2"
            ASSET -> "3"
            LIABILITY -> "4"
        }

    @StringRes
    fun toStringRes() = when (this) {
        CASH -> R.string.account_type_cash
        BANK -> R.string.account_type_bank
        CCARD -> R.string.account_type_ccard
        ASSET -> R.string.account_type_asset
        LIABILITY -> R.string.account_type_liability
    }

    companion object {
        @JvmField
        val JOIN: String = joinEnum<AccountType>(AccountType::class.java)

        fun fromQifName(qifName: String) = when (qifName) {
            "Oth L" -> LIABILITY
            "Oth A" -> ASSET
            "CCard" -> CCARD
            "Cash" -> CASH
            else -> BANK
        }

        fun sqlOrderExpression() = buildString {
            append("CASE $KEY_TYPE")
            for (type in entries) {
                append(" WHEN '${type.name}' THEN ${type.sortOrder}")
            }
            append(" ELSE -1 END AS $KEY_SORT_KEY_TYPE")
        }
    }
}
