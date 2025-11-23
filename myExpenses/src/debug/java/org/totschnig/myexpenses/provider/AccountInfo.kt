package org.totschnig.myexpenses.provider

import android.content.ContentValues

/**
 * A utility for converting account data to a ContentValues map.
 */
data class AccountInfo @JvmOverloads constructor(
    val label: String,
    val type: Long,
    val openingBalance: Long,
    val currency: String = "EUR",
    val dynamic: Boolean = false,
    val usages: Int = 0,
    val lastUsed: Long = 0,
    val description: String = "My account of type $type"
) {

    val contentValues = ContentValues().apply {
        put(KEY_LABEL, label)
        put(KEY_DESCRIPTION, description)
        put(KEY_OPENING_BALANCE, openingBalance)
        put(KEY_CURRENCY, currency)
        put(KEY_TYPE, type)
        put(KEY_DYNAMIC, dynamic)
        put(KEY_USAGES, usages)
        put(KEY_LAST_USED, lastUsed)
    }
}