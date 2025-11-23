package org.totschnig.myexpenses.viewmodel.data

import android.database.Cursor
import org.totschnig.myexpenses.provider.KEY_IS_NUMBERED
import org.totschnig.myexpenses.provider.KEY_LABEL
import org.totschnig.myexpenses.provider.KEY_ROWID
import org.totschnig.myexpenses.provider.getBoolean
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString

data class PaymentMethod(
    val id: Long,
    val label: String,
    val isNumbered: Boolean
) {
    override fun toString(): String {
        return label
    }

    companion object {
        fun from(cursor: Cursor): PaymentMethod {
            return PaymentMethod(
                id = cursor.getLong(KEY_ROWID),
                label = cursor.getString(KEY_LABEL),
                isNumbered = cursor.getBoolean(KEY_IS_NUMBERED)
            )
        }
    }
}
