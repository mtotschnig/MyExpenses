package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_DEBTS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TEMPLATES
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_MAPPED_TRANSACTIONS
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID

data class Party(
    val id: Long, val name: String,
    val mappedTransactions: Boolean,
    val mappedTemplates: Boolean,
    val mappedDebts: Boolean
) {
    override fun toString() = name

    companion object {
        fun fromCursor(cursor: Cursor) = Party(
            cursor.getLong(cursor.getColumnIndexOrThrow(KEY_ROWID)),
            cursor.getString(cursor.getColumnIndexOrThrow(KEY_PAYEE_NAME)),
            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MAPPED_TRANSACTIONS)) > 0,
            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MAPPED_TEMPLATES)) > 0,
            cursor.getInt(cursor.getColumnIndexOrThrow(KEY_MAPPED_DEBTS)) > 0
        )
    }
}