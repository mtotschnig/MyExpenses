package org.totschnig.myexpenses.model

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.os.Parcelable
import androidx.core.content.contentValuesOf
import com.google.common.collect.BiMap
import com.google.common.collect.HashBiMap
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNT_TYPE_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_IS_ASSET
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_SUPPORTS_RECONCILIATION
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE_SORT_KEY
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getIntIfExists
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString

const val PREDEFINED_NAME_CASH = "_CASH_"
const val PREDEFINED_NAME_BANK = "_BANK_"
const val PREDEFINED_NAME_CCARD = "_CCARD_"
const val PREDEFINED_NAME_ASSET = "_ASSET_"
const val PREDEFINED_NAME_LIABILITY = "_LIABILITY_"
const val PREDEFINED_NAME_INVESTMENT = "_INVST_"

@Parcelize
data class AccountType(
    override val id: Long = 0,
    val name: String,
    val sortKey: Int = 0,
    val isAsset: Boolean = true,
    val supportsReconciliation: Boolean = false,
    val count: Int? = null
) : IdHolder, Parcelable {
    @IgnoredOnParcel
    val asContentValues: ContentValues
        get() = contentValuesOf(
            KEY_LABEL to name,
            KEY_TYPE_SORT_KEY to sortKey,
            KEY_IS_ASSET to isAsset,
            KEY_SUPPORTS_RECONCILIATION to supportsReconciliation
        )

    @IgnoredOnParcel
    val isPredefined: Boolean = isReservedName(name)

    @IgnoredOnParcel
    val nameForSyncLegacy =
        if (isPredefined) name.substring(1, name.length - 1) else name

    @IgnoredOnParcel
    val qifName = qifToInternalMap.inverse()[name] ?: if (isAsset) "Oth A" else "Oth L"

    val isCashAccount: Boolean
        get() = name == PREDEFINED_NAME_CASH

    fun localizedName(context: Context) = when (name) {
        PREDEFINED_NAME_CASH -> R.string.account_type_cash
        PREDEFINED_NAME_BANK -> R.string.account_type_bank
        PREDEFINED_NAME_CCARD -> R.string.account_type_ccard
        PREDEFINED_NAME_ASSET -> R.string.account_type_asset
        PREDEFINED_NAME_LIABILITY -> R.string.account_type_liability
        PREDEFINED_NAME_INVESTMENT -> R.string.account_type_investment
        else -> 0
    }.takeIf { it != 0 }?.let { context.getString(it) } ?: name

    override fun toString() = name

    companion object {
        val qifToInternalMap: BiMap<String, String> = HashBiMap.create()

        init {
            qifToInternalMap.put("Cash", PREDEFINED_NAME_CASH)
            qifToInternalMap.put("Bank", PREDEFINED_NAME_BANK)
            qifToInternalMap.put("CCard", PREDEFINED_NAME_CCARD)
            qifToInternalMap.put("Invst", PREDEFINED_NAME_INVESTMENT)
            qifToInternalMap.put("Oth A", PREDEFINED_NAME_ASSET)
            qifToInternalMap.put("Oth L", PREDEFINED_NAME_LIABILITY)
        }

        fun qif2Internal(qifName: String) = qifToInternalMap[qifName]

        val CASH = AccountType(name = PREDEFINED_NAME_CASH, isAsset = true, supportsReconciliation = false, sortKey = 2)
        val BANK = AccountType(name = PREDEFINED_NAME_BANK, isAsset = true, supportsReconciliation = true, sortKey = 1)
        val INVESTMENT =
            AccountType(name = PREDEFINED_NAME_INVESTMENT, isAsset = true, supportsReconciliation = true, sortKey = 0)
        val ASSET = AccountType(name = PREDEFINED_NAME_ASSET, isAsset = true, supportsReconciliation = true, sortKey = -1)
        val CCARD = AccountType(name = PREDEFINED_NAME_CCARD, isAsset = false, supportsReconciliation = true, sortKey = 0)
        val LIABILITY =
            AccountType(name = PREDEFINED_NAME_LIABILITY, isAsset = false, supportsReconciliation = true, sortKey = -1)

        val initialAccountTypes = listOf(CASH, BANK, CCARD, ASSET, LIABILITY, INVESTMENT)

        fun fromCursor(cursor: Cursor) = AccountType(
            id = cursor.getLong(KEY_ROWID),
            name = cursor.getString(KEY_LABEL),
            isAsset = cursor.getBoolean(KEY_IS_ASSET),
            supportsReconciliation = cursor.getBoolean(KEY_SUPPORTS_RECONCILIATION),
            count = cursor.getIntIfExists(KEY_COUNT)
        )

        fun withName(name: String) = AccountType(name = name)

        fun fromAccountCursor(cursor: Cursor) = AccountType(
            id = cursor.getLong(KEY_TYPE),
            name = cursor.getString(KEY_ACCOUNT_TYPE_LABEL),
            isAsset = cursor.getBoolean(KEY_IS_ASSET),
            supportsReconciliation = cursor.getBoolean(KEY_SUPPORTS_RECONCILIATION)
        )

        fun isReservedName(name: String) = name.startsWith("_") && name.endsWith("_")
    }
}