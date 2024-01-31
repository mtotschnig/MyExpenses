package org.totschnig.myexpenses.model

import android.content.ContentProviderOperation
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.fromSyncAdapter

/**
 * Can only be used from SyncAdapter or tests
 */
fun saveTagLinks(
    tagIds: List<Long>,
    transactionId: Long?,
    backReference: Int? = null,
    replace: Boolean = true
) = buildList {
    val uri = TransactionProvider.TRANSACTIONS_TAGS_URI.fromSyncAdapter()
    if (replace) {
        transactionId?.let {
            add(
                ContentProviderOperation.newDelete(uri)
                    .withSelection("$KEY_TRANSACTIONID = ?", arrayOf(it.toString())).build()
            )
        }
    }
    tagIds.forEach { tagId ->
        val insert =
            ContentProviderOperation.newInsert(uri)
                .withValue(KEY_TAGID, tagId)
        transactionId?.let {
            insert.withValue(KEY_TRANSACTIONID, it)
        } ?: backReference?.let {
            insert.withValueBackReference(KEY_TRANSACTIONID, it)
        } ?: throw IllegalArgumentException("neither id nor backReference provided")
        add(insert.build())
    }
}