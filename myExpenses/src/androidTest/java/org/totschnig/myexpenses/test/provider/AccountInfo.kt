package org.totschnig.myexpenses.test.provider

import kotlin.jvm.JvmOverloads
import android.content.ContentValues
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DatabaseConstants

data class AccountInfo @JvmOverloads internal constructor(
    val label: String,
    val type: AccountType,
    val openingBalance: Long,
    val currency: String = "EUR",
    val usages: Int = 0,
    val lastUsed: Long = 0
) {

    val contentValues: ContentValues
        get() = ContentValues().apply {
            put(DatabaseConstants.KEY_LABEL, label)
            put(DatabaseConstants.KEY_DESCRIPTION, description)
            put(DatabaseConstants.KEY_OPENING_BALANCE, openingBalance)
            put(DatabaseConstants.KEY_CURRENCY, currency)
            put(DatabaseConstants.KEY_TYPE, type.name)
            put(DatabaseConstants.KEY_USAGES, usages)
            put(DatabaseConstants.KEY_LAST_USED, lastUsed)
        }

    val description: String
        get() = "My account of type " + type.name
}