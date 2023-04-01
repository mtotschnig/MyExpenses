package org.totschnig.myexpenses.model

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.net.Uri
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.useAndMap
import org.totschnig.myexpenses.viewmodel.data.Tag

@Deprecated("Use methods in RepositoryTags")
fun loadTags(linkUri: Uri, column: String, id: Long, contentResolver: ContentResolver): List<Tag> =
        //noinspection Recycle
        contentResolver.query(linkUri, null, "$column = ?", arrayOf(id.toString()), null)!!.useAndMap {
                Tag(
                    id = it.getLong(DatabaseConstants.KEY_ROWID),
                    label = it.getString(DatabaseConstants.KEY_LABEL),
                    count = 0
                )
            }

@Deprecated("Use methods in RepositoryTags")
fun saveTags(linkUri: Uri, column: String, tags: List<Tag>?, id: Long, contentResolver: ContentResolver): Boolean {
    val ops = ArrayList<ContentProviderOperation>()
    ops.add(ContentProviderOperation.newDelete(linkUri)
            .withSelection("$column = ?", arrayOf(id.toString()))
            .build())
    tags?.forEach {
        ops.add(ContentProviderOperation.newInsert(linkUri)
            .withValue(column, id)
            .withValue(DatabaseConstants.KEY_TAGID, it.id).build())
    }

    return contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops).size == ops.size
}