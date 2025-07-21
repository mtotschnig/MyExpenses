package org.totschnig.myexpenses.model

import android.database.Cursor
import android.os.Parcelable
import androidx.core.content.contentValuesOf
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_ASSET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUPPORTS_RECONCILIATION
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString

@Parcelize
data class AccountType(
    override val id: Long = 0,
    val name: String,
    val isAsset: Boolean = true,
    val supportsReconciliation: Boolean = false,
) : IdHolder, Parcelable {
    @IgnoredOnParcel
    val asContentValues = contentValuesOf(
        KEY_LABEL to name,
        KEY_IS_ASSET to isAsset,
        KEY_SUPPORTS_RECONCILIATION to supportsReconciliation
    )

    @IgnoredOnParcel
    val nameForSync =
        if (name.startsWith("_") && name.endsWith("_")) name.substring(1, name.length - 1) else name

    @IgnoredOnParcel
    val qifName = when (name) {
        "_CASH_" -> "Cash"
        "_BANK_" -> "Bank"
        "_CCARD_" -> "CCard"
        "_ASSET_" -> "Oth A"
        "_LIABILITY_" -> "Oth L"
        "_INVST_" -> "Invst"
        else -> if (isAsset) "Oth A" else "Oth L"
    }

    val localizedName = when (name) {
        "_CASH_" -> R.string.account_type_cash
        "_BANK_" -> R.string.account_type_bank
        "_CCARD_" -> R.string.account_type_ccard
        "_ASSET_" -> R.string.account_type_asset
        "_LIABILITY_" -> R.string.account_type_liability
        "_INVST_" -> R.string.account_type_investment
        else -> 0
    }

    val isCashAccount: Boolean
        get() = name == "_CASH_"

    companion object {
        val CASH = AccountType(name = "_CASH_", isAsset = true, supportsReconciliation = false)
        val BANK = AccountType(name = "_BANK_", isAsset = true, supportsReconciliation = true)
        val CCARD = AccountType(name = "_CCARD_", isAsset = false, supportsReconciliation = true)
        val ASSET = AccountType(name = "_ASSET_", isAsset = true, supportsReconciliation = true)
        val LIABILITY =
            AccountType(name = "_LIABILITY_", isAsset = false, supportsReconciliation = true)
        val INVESTMENT =
            AccountType(name = "_INVST_", isAsset = true, supportsReconciliation = true)

        val predefinedAccounts = listOf(CASH, BANK, CCARD, ASSET, LIABILITY, INVESTMENT)

        fun fromCursor(cursor: Cursor) = AccountType(
            id = cursor.getLong(KEY_ROWID),
            name = cursor.getString(KEY_LABEL),
            isAsset = cursor.getBoolean(KEY_IS_ASSET),
            supportsReconciliation = cursor.getBoolean(KEY_SUPPORTS_RECONCILIATION)
        )
    }
}