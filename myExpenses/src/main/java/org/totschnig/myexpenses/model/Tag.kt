package org.totschnig.myexpenses.model

import android.content.ContentProviderOperation
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider

fun saveTagLinks(
    tagIds: List<Long>?,
    transactionId: Long?,
    backReference: Int?,
    replace: Boolean = true
) = buildList {
    if (replace) {
        transactionId?.let {
            add(
                ContentProviderOperation.newDelete(TransactionProvider.TRANSACTIONS_TAGS_URI)
                    .withSelection("$KEY_TRANSACTIONID = ?", arrayOf(it.toString())).build()
            )
        }
    }
    tagIds?.forEach { tagId ->
        val insert =
            ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_TAGS_URI)
                .withValue(KEY_TAGID, tagId)
        transactionId?.let {
            insert.withValue(KEY_TRANSACTIONID, it)
        } ?: backReference?.let {
            insert.withValueBackReference(KEY_TRANSACTIONID, it)
        } ?: throw IllegalArgumentException("neither id nor backReference provided")
        add(insert.build())
    }
}