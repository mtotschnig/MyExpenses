package org.totschnig.myexpenses.model2

import android.database.Cursor
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BANK_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BIC
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_BLZ
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_USER_ID
import org.totschnig.myexpenses.provider.getInt
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString

@Parcelize
data class Bank(
    val id: Long = 0L,
    val blz: String,
    val bic: String,
    val bankName: String,
    val userId: String,
    /**
     * holds the number of accounts linked to this bank
     */
    val count: Int = 0
): Parcelable {
    override fun toString(): String {
        return "$bankName ($userId)"
    }

    companion object {
        fun fromCursor(cursor: Cursor) = Bank(
            id = cursor.getLong(KEY_ROWID),
            blz = cursor.getString(KEY_BLZ),
            bic = cursor.getString(KEY_BIC),
            bankName = cursor.getString(KEY_BANK_NAME),
            userId = cursor.getString(KEY_USER_ID),
            count = cursor.getInt(KEY_COUNT)
        )
    }
}