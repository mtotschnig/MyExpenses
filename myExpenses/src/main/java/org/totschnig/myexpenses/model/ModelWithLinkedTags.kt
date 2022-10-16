package org.totschnig.myexpenses.model

import android.content.ContentProviderOperation
import android.net.Uri
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.viewmodel.data.Tag

fun loadTags(linkUri: Uri, column: String, id: Long): List<Tag>? =
        Model.cr().query(linkUri, null, "$column = ?", arrayOf(id.toString()), null)?.use { cursor ->
            cursor.asSequence.map {
                Tag(it.getLong(it.getColumnIndexOrThrow(DatabaseConstants.KEY_ROWID)), it.getString(it.getColumnIndexOrThrow(DatabaseConstants.KEY_LABEL)), 0)
            }.toList()
        }

fun saveTags(linkUri: Uri, column: String, tags: List<Tag>?, id: Long): Boolean {
    val ops = ArrayList<ContentProviderOperation>()
    ops.add(ContentProviderOperation.newDelete(linkUri)
            .withSelection("$column = ?", arrayOf(id.toString()))
            .build())
    tags?.forEach {
        ops.add(ContentProviderOperation.newInsert(linkUri)
            .withValue(column, id)
            .withValue(DatabaseConstants.KEY_TAGID, it.id).build())
    }

    return Model.cr().applyBatch(TransactionProvider.AUTHORITY, ops).size == ops.size
}