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
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ACCOUNTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PAYEE_NAME
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TAGLIST
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TEMPLATEID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TRANSACTIONID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.ACCOUNTS_TAGS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_SAVE_TRANSACTION_TAGS
import org.totschnig.myexpenses.provider.TransactionProvider.PAYEES_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TAGS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TEMPLATES_TAGS_URI
import org.totschnig.myexpenses.provider.TransactionProvider.TRANSACTIONS_TAGS_URI
import org.totschnig.myexpenses.provider.getIntOrNull
import org.totschnig.myexpenses.provider.getString
import org.totschnig.myexpenses.provider.useAndMapToList
import org.totschnig.myexpenses.sync.json.TagInfo
import org.totschnig.myexpenses.viewmodel.data.Tag
import java.io.IOException


fun Repository.loadActiveTagsForAccount(accountId: Long) =
    contentResolver.loadTags(ACCOUNTS_TAGS_URI, KEY_ACCOUNTID, accountId)

fun ContentResolver.loadTagsForTransaction(transactionId: Long) =
    loadTags(TRANSACTIONS_TAGS_URI, KEY_TRANSACTIONID, transactionId)

fun ContentResolver.loadTagsForTemplate(templateId: Long) =
    loadTags(TEMPLATES_TAGS_URI, KEY_TEMPLATEID, templateId)

fun Repository.saveActiveTagsForAccount(tags: List<Tag>?, accountId: Long) =
    contentResolver.saveTags(ACCOUNTS_TAGS_URI, KEY_ACCOUNTID, tags, accountId)

fun Repository.saveTagsForTransaction(tags: List<Tag>, transactionId: Long) {
    contentResolver.saveTagsForTransaction(tags.map { it.id }.toLongArray(), transactionId)
}

fun ContentResolver.saveTagsForTransaction(tags: LongArray, transactionId: Long) {
    call(
        DUAL_URI,
        METHOD_SAVE_TRANSACTION_TAGS,
        null,
        Bundle().apply {
            putLong(KEY_TRANSACTIONID, transactionId)
            putLongArray(KEY_TAGLIST, tags)
        }
    )
}

fun ContentResolver.saveTagsForTemplate(tags: List<Tag>?, templateId: Long) =
    saveTags(TEMPLATES_TAGS_URI, KEY_TEMPLATEID, tags, templateId)

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

fun Repository.extractTagIdsV2(tags: Collection<TagInfo>, tagToId: MutableMap<String, Long>) =
    tags.map { tag ->
        tagToId[tag.label] ?: extractTagId(tag).also { tagToId[tag.label] = it }
    }

fun Repository.extractTagId(tagInfo: TagInfo): Long {
    val existingTag = find(tagInfo.label)
    return if (existingTag != null) {
        if (existingTag.second != tagInfo.color) {
           contentResolver.update(ContentUris.withAppendedId(TAGS_URI, existingTag.first),
               ContentValues(1).apply {
                   put(KEY_COLOR, tagInfo.color)
               }, null, null
               )
        }
        existingTag.first
    } else
        writeTag(tagInfo.label, tagInfo.color)
}

fun Repository.extractTagId(label: String) =
    find(label)?.first ?: writeTag(label)

/**
 * Looks for a tag with label
 * @return pair of id and color or null if not found
 */
private fun Repository.find(label: String) = contentResolver.query(
    TAGS_URI,
    arrayOf(KEY_ROWID, KEY_COLOR),
    "$KEY_LABEL = ?",
    arrayOf(label.trim()),
    null
)?.use {
    if (it.moveToFirst())
        it.getLong(0) to it.getInt(1) else null
}

@VisibleForTesting
fun Repository.writeTag(label: String, colorInt: Int? = null) =
    ContentUris.parseId(contentResolver.insert(
        TAGS_URI,
        ContentValues().apply {
            put(KEY_LABEL, label.trim())
            colorInt?.let {
                put(KEY_COLOR, it)
            }
        }
    )!!)

@VisibleForTesting
fun Repository.deleteAllTags() {
    contentResolver.delete(TAGS_URI, null, null)
}

/**
 * Map of tag id to pair (label, color)
 */
val ContentResolver.tagMapFlow: Flow<Map<String, Pair<String, Int?>>>
    get() = observeQuery(TAGS_URI, notifyForDescendants = true)
        .transform { query ->
            val map = withContext(Dispatchers.IO) {
                query.run()?.use(Cursor::toTagMap)
            }
            if (map != null) {
                emit(map)
            }
        }

val ContentResolver.tagMap: Map<String, Pair<String, Int?>>
    get() = query(TAGS_URI, null, null, null, null)!!
        .use { it.toTagMap() }

fun Cursor.toTagMap() = buildMap {
    while (moveToNext()) {
        put(
            getString(KEY_ROWID),
            (getString(KEY_LABEL) to
                    getIntOrNull(KEY_COLOR))
        )
    }
}

fun Repository.getTag(tagId: Long) = contentResolver.query(
    ContentUris.withAppendedId(TAGS_URI, tagId),
    arrayOf(KEY_LABEL), null, null, null
)?.use {
    it.moveToFirst()
    it.getString(0)
}