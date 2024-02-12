package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentResolver
import android.content.ContentUris
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import app.cash.copper.flow.observeQuery
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.withContext
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.io.IOException


fun Repository.loadActiveTagsForAccount(accountId: Long) =
    contentResolver.loadTags(TransactionProvider.ACCOUNTS_TAGS_URI, KEY_ACCOUNTID, accountId)

fun ContentResolver.loadTagsForTransaction(transactionId: Long) =
    loadTags(TransactionProvider.TRANSACTIONS_TAGS_URI, KEY_TRANSACTIONID, transactionId)

fun ContentResolver.loadTagsForTemplate(templateId: Long) =
    loadTags(TransactionProvider.TEMPLATES_TAGS_URI, KEY_TEMPLATEID, templateId)

fun Repository.saveActiveTagsForAccount(tags: List<Tag>?, accountId: Long) =
    contentResolver.saveTags(TransactionProvider.ACCOUNTS_TAGS_URI, KEY_ACCOUNTID, tags, accountId)

fun Repository.saveTagsForTransaction(tags: List<Tag>, transactionId: Long) {
    contentResolver.saveTagsForTransaction(tags.map { it.id }.toLongArray(), transactionId)
}

fun ContentResolver.saveTagsForTransaction(tags: LongArray, transactionId: Long) {
    call(
        TransactionProvider.DUAL_URI,
        TransactionProvider.METHOD_SAVE_TRANSACTION_TAGS,
        null,
        Bundle().apply {
            putLong(KEY_TRANSACTIONID, transactionId)
            putLongArray(KEY_TAGLIST, tags)
        }
    )
}

fun ContentResolver.saveTagsForTemplate(tags: List<Tag>?, templateId: Long) =
    saveTags(TransactionProvider.TEMPLATES_TAGS_URI, KEY_TEMPLATEID, tags, templateId)

private fun ContentResolver.loadTags(linkUri: Uri, column: String, id: Long): List<Tag> =
    //noinspection Recycle
    query(linkUri, null, "$column = ?", arrayOf(id.toString()), null)!!
        .useAndMapToList(Tag.Companion::fromCursor)

private fun ContentResolver.saveTags(linkUri: Uri, column: String, tags: List<Tag>?, id: Long) {
    val ops = ArrayList<ContentProviderOperation>()
    ops.add(
        ContentProviderOperation.newDelete(linkUri)
            .withSelection("$column = ?", arrayOf(id.toString()))
            .build()
    )
    tags?.forEach {
        ops.add(
            ContentProviderOperation.newInsert(linkUri)
                .withValue(column, id)
                .withValue(KEY_TAGID, it.id)
                .build()
        )
    }

    if (applyBatch(TransactionProvider.AUTHORITY, ops).size != ops.size) {
        throw IOException("Saving tags failed")
    }
}

fun Repository.extractTagIds(tags: Collection<String?>, tagToId: MutableMap<String, Long>) =
    tags.filterNotNull().map { tag ->
        tagToId[tag] ?: extractTagId(tag).also { tagToId[tag] = it }
    }

private fun Repository.extractTagId(label: String) =
    find(label).takeIf { it > -1 } ?: writeTag(label)

/**
 * Looks for a tag with label
 *
 * @param label
 * @return id or -1 if not found
 */
private fun Repository.find(label: String): Long {
    val selection = "$KEY_LABEL = ?"
    val selectionArgs = arrayOf(label.trim())
    contentResolver.query(
        TransactionProvider.TAGS_URI,
        arrayOf(KEY_ROWID),
        selection,
        selectionArgs,
        null
    )?.use {
        if (it.moveToFirst())
            return it.getLong(0)
    }
    return -1
}

@VisibleForTesting
fun Repository.writeTag(label: String) =
    contentResolver.insert(
        TransactionProvider.TAGS_URI,
        ContentValues().apply { put(KEY_LABEL, label.trim()) }
    )?.let {
        ContentUris.parseId(it)
    } ?: -1

/**
 * Map of tag id to pair (label, color)
 */
val ContentResolver.tagMapFlow: Flow<Map<String, Pair<String, Int?>>>
    get() = observeQuery(TransactionProvider.TAGS_URI, notifyForDescendants = true)
        .transform { query ->
            val map = withContext(Dispatchers.IO) {
                query.run()?.use(Cursor::toTagMap)
            }
            if (map != null) {
                emit(map)
            }
        }

val ContentResolver.tagMap: Map<String, Pair<String, Int?>>
    get() = query(TransactionProvider.TAGS_URI, null, null, null, null)!!
        .use(Cursor::toTagMap)

fun Cursor.toTagMap() = buildMap {
    while (moveToNext()) {
        put(
            getString(KEY_ROWID),
            (getString(KEY_LABEL) to
                    getIntOrNull(DatabaseConstants.KEY_COLOR))
        )
    }
}
