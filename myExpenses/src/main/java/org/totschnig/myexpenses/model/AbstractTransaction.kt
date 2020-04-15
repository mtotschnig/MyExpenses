package org.totschnig.myexpenses.model

import android.content.ContentProviderOperation
import android.content.ContentResolver
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.viewmodel.data.Tag

/**
 * This class allows us to write new features for Transaction in Kotlin without migrating the whole class.
 */
abstract class AbstractTransaction: Model(), ITransaction {
     override fun linkedTagsUri() = TransactionProvider.TRANSACTIONS_TAGS_URI
     override fun linkColumn() = DatabaseConstants.KEY_TRANSACTIONID

     override fun saveTags(tags: List<Tag>?, contentResolver: ContentResolver): Boolean {
        val ops = ArrayList<ContentProviderOperation>()
        ops.add(ContentProviderOperation.newDelete(linkedTagsUri())
                .withSelection(linkColumn() + " = ?", arrayOf(id.toString()))
                .build())
        tags?.let {
            val (newTags, existingTags) = it.partition { tag -> tag.id == -1L }

            newTags.forEachIndexed { index, tag ->
                ops.add(ContentProviderOperation.newInsert(TransactionProvider.TAGS_URI).withValue(DatabaseConstants.KEY_LABEL, tag.label.trim()).build())
                ops.add(ContentProviderOperation.newInsert(linkedTagsUri())
                        .withValue(linkColumn(), id)
                        //first operation is delete
                        .withValueBackReference(DatabaseConstants.KEY_TAGID, 1 + index * 2).build())
            }
            for (tag in existingTags) {
                ops.add(ContentProviderOperation.newInsert(linkedTagsUri())
                        .withValue(linkColumn(), id)
                        .withValue(DatabaseConstants.KEY_TAGID, tag.id).build())
            }
        }
        return contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops).size == ops.size
    }
}