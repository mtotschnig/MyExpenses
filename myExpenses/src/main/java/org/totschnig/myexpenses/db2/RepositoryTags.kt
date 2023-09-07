package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentUris
import android.content.ContentValues
import android.net.Uri
import androidx.annotation.VisibleForTesting
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getLong
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.useAndMap
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.io.IOException

fun Repository.loadActiveTagsForAccount(accountId: Long) =
    loadTags(TransactionProvider.ACCOUNTS_TAGS_URI, DatabaseConstants.KEY_ACCOUNTID, accountId)

fun Repository.loadTagsForTransaction(transactionId: Long) =
    loadTags(TransactionProvider.TRANSACTIONS_TAGS_URI, DatabaseConstants.KEY_TRANSACTIONID, transactionId)

fun Repository.loadTagsForTemplate(templateId: Long) =
    loadTags(TransactionProvider.TEMPLATES_TAGS_URI, DatabaseConstants.KEY_TEMPLATEID, templateId)

fun Repository.saveActiveTagsForAccount(tags: List<Tag>?, accountId: Long) =
    saveTags(TransactionProvider.ACCOUNTS_TAGS_URI, DatabaseConstants.KEY_ACCOUNTID, tags, accountId)

fun Repository.saveTagsForTransaction(tags: List<Tag>?, transactionId: Long) =
    saveTags(TransactionProvider.TRANSACTIONS_TAGS_URI, DatabaseConstants.KEY_TRANSACTIONID, tags, transactionId)

fun Repository.saveTagsForTemplate(tags: List<Tag>?, templateId: Long) =
    saveTags(TransactionProvider.TEMPLATES_TAGS_URI, DatabaseConstants.KEY_TEMPLATEID, tags, templateId)

private fun Repository.loadTags(linkUri: Uri, column: String, id: Long): List<Tag> =
    //noinspection Recycle
    contentResolver.query(linkUri, null, "$column = ?", arrayOf(id.toString()), null)!!.useAndMap {
        Tag(
            id = it.getLong(DatabaseConstants.KEY_ROWID),
            label = it.getString(DatabaseConstants.KEY_LABEL),
            count = 0
        )
    }

private fun Repository.saveTags(linkUri: Uri, column: String, tags: List<Tag>?, id: Long) {
    val ops = ArrayList<ContentProviderOperation>()
    ops.add(
        ContentProviderOperation.newDelete(linkUri)
        .withSelection("$column = ?", arrayOf(id.toString()))
        .build())
    tags?.forEach {
        ops.add(
            ContentProviderOperation.newInsert(linkUri)
            .withValue(column, id)
            .withValue(DatabaseConstants.KEY_TAGID, it.id)
            .build())
    }

   if (contentResolver.applyBatch(TransactionProvider.AUTHORITY, ops).size != ops.size) {
       throw IOException("Saving tags failed")
   }
}

fun Repository.extractTagIds(tags: Collection<String?>, tagToId: MutableMap<String, Long>) =
    tags.filterNotNull().map { tag ->
        tagToId[tag] ?: extractTagId(tag).also { tagToId[tag] = it }
    }

private fun Repository.extractTagId(label: String) = find(label).takeIf { it > -1 } ?: writeTag(label)

/**
 * Looks for a tag with label
 *
 * @param label
 * @return id or -1 if not found
 */
private fun Repository.find(label: String): Long {
    val selection = "${DatabaseConstants.KEY_LABEL} = ?"
    val selectionArgs = arrayOf(label.trim())
    contentResolver.query(TransactionProvider.TAGS_URI, arrayOf(DatabaseConstants.KEY_ROWID), selection, selectionArgs, null)?.use {
        if (it.moveToFirst())
            return it.getLong(0)
    }
    return -1
}

@VisibleForTesting
fun Repository.writeTag(label: String) =
    contentResolver.insert(
        TransactionProvider.TAGS_URI,
        ContentValues().apply { put(DatabaseConstants.KEY_LABEL, label.trim()) }
    )?.let {
        ContentUris.parseId(it)
    } ?: -1