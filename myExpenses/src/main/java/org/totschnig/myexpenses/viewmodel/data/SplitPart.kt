package org.totschnig.myexpenses.viewmodel.data

import android.content.ContentResolver
import android.database.Cursor
import org.totschnig.myexpenses.adapter.SplitPartRVAdapter
import org.totschnig.myexpenses.db2.loadTagsForTransaction
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getStringIfExists
import org.totschnig.myexpenses.provider.getStringOrNull

data class SplitPart(
    override val id: Long,
    override val amountRaw: Long,
    override val comment: String?,
    override val categoryPath: String?,
    override val transferAccount: String?,
    override val debtLabel: String?,
    override val tagList: List<Tag>,
    override val icon: String?
) : SplitPartRVAdapter.ITransaction {
    companion object {
        fun fromCursor(cursor: Cursor, contentResolver: ContentResolver) = with(cursor) {
            val id = getLong(DatabaseConstants.KEY_ROWID)
            SplitPart(
                id,
                getLong(DatabaseConstants.KEY_AMOUNT),
                getStringOrNull(DatabaseConstants.KEY_COMMENT),
                getStringOrNull(DatabaseConstants.KEY_PATH),
                getStringOrNull(DatabaseConstants.KEY_TRANSFER_ACCOUNT_LABEL),
                getStringIfExists(BaseTransactionProvider.KEY_DEBT_LABEL),
                contentResolver.loadTagsForTransaction(id),
                getStringOrNull(DatabaseConstants.KEY_ICON)
            )
        }
    }
}