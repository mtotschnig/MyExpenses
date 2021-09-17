package org.totschnig.myexpenses.test.provider

import kotlin.jvm.JvmOverloads
import android.content.ContentValues
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.model.Grouping
import org.totschnig.myexpenses.provider.DatabaseConstants

data class AccountInfo @JvmOverloads internal constructor(
    val label: String,
    val type: AccountType,
    val openingBalance: Long,
    val currency: String = "EUR",
    val grouping: Grouping = Grouping.NONE
) {
    val contentValues: ContentValues
        get() {
            val v = ContentValues()
            v.put(DatabaseConstants.KEY_LABEL, label)
            v.put(DatabaseConstants.KEY_DESCRIPTION, description)
            v.put(DatabaseConstants.KEY_OPENING_BALANCE, openingBalance)
            v.put(DatabaseConstants.KEY_CURRENCY, currency)
            v.put(DatabaseConstants.KEY_TYPE, type.name)
            v.put(DatabaseConstants.KEY_GROUPING, grouping.name)
            return v
        }
    val description: String
        get() = "My account of type " + type.name
}