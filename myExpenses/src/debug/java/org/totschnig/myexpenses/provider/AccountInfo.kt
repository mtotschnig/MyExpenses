package org.totschnig.myexpenses.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants.*

/**
 * A utility for converting account data to a ContentValues map.
 */
data class AccountInfo(
    val label: String?,
    val type: AccountType,
    val openingBalance: Long,
    val currency: String? = "EUR",
    val dynamic: Boolean = false
) {

    val contentValues = ContentValues().apply {
        put(KEY_LABEL, label)
        put(KEY_DESCRIPTION, getDescription())
        put(KEY_OPENING_BALANCE, openingBalance)
        put(KEY_CURRENCY, currency)
        put(KEY_TYPE, type.name)
        put(KEY_DYNAMIC, dynamic)
    }

    fun getDescription(): String {
        return "My account of type " + type.name
    }
}