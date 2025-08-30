package org.totschnig.myexpenses.viewmodel.data

import android.content.Context
import android.database.Cursor
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.adapter.IdHolder
import org.totschnig.myexpenses.model.AccountFlag
import org.totschnig.myexpenses.model.AccountType
import org.totschnig.myexpenses.provider.DataBaseAccount.Companion.HOME_AGGREGATE_ID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CURRENCY
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString

data class AccountMinimal(
    override val id: Long,
    val label: String,
    val currency: String,
    val type: AccountType?,
    val flag: AccountFlag?
) : IdHolder {
    override fun toString(): String {
        return label
    }

    companion object {
        fun fromCursor(context: Context, cursor: Cursor): AccountMinimal {
            val id = cursor.getLong(KEY_ROWID)
            return AccountMinimal(
                id = id,
                label = if (id == HOME_AGGREGATE_ID)
                    context.getString(R.string.grand_total)
                else
                    cursor.getString(KEY_LABEL),
                currency = cursor.getString(KEY_CURRENCY),
                type = if (id < 0) null else AccountType.fromAccountCursor(cursor),
                flag = if (id < 0) null else AccountFlag.fromAccountCursor(cursor)
            )
        }
    }
}