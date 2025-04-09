package org.totschnig.myexpenses.provider

import android.content.ContentValues
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants.*

/**
 * A utility for converting account data to a ContentValues map.
 */
data class AccountInfo @JvmOverloads constructor(
    val label: String,
    val type: AccountType,
    val openingBalance: Long,
    val currency: String = "EUR",
    val dynamic: Boolean = false,
    val usages: Int = 0,
    val lastUsed: Long = 0
) {

    val contentValues = ContentValues().apply {
        put(KEY_LABEL, label)
        put(KEY_DESCRIPTION, description)
        put(KEY_OPENING_BALANCE, openingBalance)
        put(KEY_CURRENCY, currency)
        put(KEY_TYPE, type.name)
        put(KEY_DYNAMIC, dynamic)
        put(KEY_USAGES, usages)
        put(KEY_LAST_USED, lastUsed)    }

    val description: String
        get() = "My account of type " + type.name
}