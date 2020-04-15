package org.totschnig.myexpenses.model

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import java.lang.IllegalArgumentException

/**
 * Looks for a tag with label
 *
 * @param name
 * @return id or -1 if not found
 */
private fun find(label: String): Long {
    val selection = KEY_LABEL + " = ?"
    val selectionArgs = arrayOf(label)
    Model.cr().query(TransactionProvider.TAGS_URI, arrayOf(DatabaseConstants.KEY_ROWID), selection, selectionArgs, null)?.use {
        if (it.moveToFirst())
            return it.getLong(0)
    }
    return -1;
}

fun extractTagIds(tags: List<String?>, tagToId: MutableMap<String, Long>) =
        tags.filter { it != null } .map { tag -> tagToId[tag] ?: extractTagId(tag!!).also { tagToId.put(tag, it) }  }

fun saveTagLinks(tagIds: List<Long>?, transactionId: Long?, backReference: Int?, ops: MutableList<ContentProviderOperation>) {
    transactionId?.let {
        ops.add(ContentProviderOperation.newDelete(TransactionProvider.TRANSACTIONS_TAGS_URI)
                .withSelection(KEY_TRANSACTIONID + " = ?", arrayOf(it.toString())).build())
    }
    tagIds?.forEach {
        val insert = ContentProviderOperation.newInsert(TransactionProvider.TRANSACTIONS_TAGS_URI).withValue(KEY_TAGID, it);
        transactionId?.let {
            insert.withValue(KEY_TRANSACTIONID, it)
        } ?: backReference?.let {
            insert.withValueBackReference(KEY_TRANSACTIONID, it)
        } ?: throw IllegalArgumentException("neither id nor backreference provided")
        ops.add(insert.build())
    }
}

private fun extractTagId(label: String) = find(label).takeIf { it > -1 } ?: write(label)

private fun write(label: String) =
        Model.cr().insert(
                TransactionProvider.TAGS_URI,
                ContentValues().apply { put(KEY_LABEL, label.trim()) }
        )?.let {
            ContentUris.parseId(it)
        } ?: -1