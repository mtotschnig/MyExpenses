package org.totschnig.myexpenses.db2

import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import org.totschnig.myexpenses.model2.Category
import org.totschnig.myexpenses.model2.CategoryExport
import org.totschnig.myexpenses.model2.CategoryInfo
import org.totschnig.myexpenses.model2.CategoryPath
import org.totschnig.myexpenses.provider.BaseTransactionProvider
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COUNT
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.provider.TransactionProvider.DUAL_URI
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_CATEGORY
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_CATEGORY_EXPORT
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_CATEGORY_INFO
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_MERGE_SOURCE
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_MERGE_TARGET
import org.totschnig.myexpenses.provider.TransactionProvider.KEY_RESULT
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_ENSURE_CATEGORY
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_ENSURE_CATEGORY_TREE
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_MERGE_CATEGORIES
import org.totschnig.myexpenses.provider.TransactionProvider.METHOD_SAVE_CATEGORY
import org.totschnig.myexpenses.provider.TransactionProvider.TAGS_URI
import org.totschnig.myexpenses.provider.asSequence
import org.totschnig.myexpenses.provider.getString
import kotlin.collections.fold
import kotlin.experimental.or

const val FLAG_TRANSFER: Byte = 0
const val FLAG_EXPENSE: Byte = 1
const val FLAG_INCOME: Byte = 2
val FLAG_NEUTRAL = FLAG_EXPENSE or FLAG_INCOME

const val DEFAULT_CATEGORY_PATH_SEPARATOR = " > "

val Boolean.asCategoryType: Byte
    get() = if (this) FLAG_INCOME else FLAG_EXPENSE

fun Repository.saveCategory(category: Category): Long? {
    val result = contentResolver.call(DUAL_URI, METHOD_SAVE_CATEGORY, null, Bundle().apply {
        putParcelable(KEY_CATEGORY, category)
    })!!
    return if (result.containsKey(KEY_ROWID)) result.getLong(KEY_ROWID) else null
}

fun Repository.moveCategory(source: Long, target: Long?) = try {
    contentResolver.update(
        TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(source.toString())
            .build(),
        ContentValues().apply {
            put(KEY_PARENTID, target)
        },
        null,
        null
    ) > 0
} catch (e: SQLiteConstraintException) {
    false
}

fun Repository.mergeCategories(source: List<Long>, target: Long) {
    contentResolver.call(DUAL_URI, METHOD_MERGE_CATEGORIES, null, Bundle().apply {
        putLongArray(KEY_MERGE_SOURCE, source.toLongArray())
        putLong(KEY_MERGE_TARGET, target)
    })
}

fun Repository.deleteCategory(id: Long) = contentResolver.delete(
    ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, id),
    null,
    null
) > 0

fun Repository.updateCategoryColor(id: Long, color: Int?) = contentResolver.update(
    ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, id),
    ContentValues().apply {
        put(KEY_COLOR, color)
    }, null, null
) == 1

/**
 * Looks for a cat with a label under a given parent
 *
 * @return id or -1 if not found
 */
fun Repository.findCategory(label: String, parentId: Long? = null): Long {
    val stripped = label.trim()
    val (parentSelection, parentSelectionArgs) = if (parentId == null) {
        "$KEY_PARENTID is null" to emptyArray()
    } else {
        "$KEY_PARENTID = ?" to arrayOf(parentId.toString())
    }
    return contentResolver.query(
        TransactionProvider.CATEGORIES_URI,
        arrayOf(KEY_ROWID),
        "$KEY_LABEL = ? AND $parentSelection",
        arrayOf(stripped) + parentSelectionArgs,
        null
    )?.use {
        if (it.count == 0) -1 else {
            it.moveToFirst()
            it.getLong(0)
        }
    } ?: -1
}

fun Repository.ensureCategoryTree(categoryExport: CategoryExport): Int {
    return contentResolver.call(DUAL_URI, METHOD_ENSURE_CATEGORY_TREE, null, Bundle().apply {
        putParcelable(KEY_CATEGORY_EXPORT, categoryExport)
    })!!.getInt(KEY_COUNT)
}

fun Repository.ensureCategory(categoryInfo: CategoryInfo, parentId: Long?) =
    contentResolver.call(DUAL_URI, METHOD_ENSURE_CATEGORY, null, Bundle().apply {
        putParcelable(KEY_CATEGORY_INFO, categoryInfo)
        parentId?.let { putLong(KEY_PARENTID, it) }
    })!!.getSerializable(KEY_RESULT) as Pair<Long, Boolean>

fun Repository.getCategoryPath(id: Long) = contentResolver.query(
    ContentUris.withAppendedId(BaseTransactionProvider.CATEGORY_TREE_URI, id),
    null, null, null, null
)?.use { cursor ->
    cursor
        .takeIf { it.count > 0 }
        ?.asSequence
        ?.map { it.getString(KEY_LABEL) }
        ?.toList()
        ?.asReversed()
        ?.joinToString(DEFAULT_CATEGORY_PATH_SEPARATOR)
}

fun Repository.getCategoryInfoList(id: Long): CategoryPath? = contentResolver.query(
    ContentUris.withAppendedId(BaseTransactionProvider.CATEGORY_TREE_URI, id),
    null, null, null, null
)?.use { cursor -> CategoryInfo.fromCursor(cursor) }

fun Repository.ensureCategoryPath(categoryPath: CategoryPath) =
    categoryPath.fold(null) { parentId: Long?, categoryInfo: CategoryInfo ->
        ensureCategory(categoryInfo, parentId).first
}

@VisibleForTesting
fun Repository.loadCategory(id: Long): Category? = contentResolver.query(
    TransactionProvider.CATEGORIES_URI,
    arrayOf(
        KEY_PARENTID,
        KEY_LABEL,
        KEY_COLOR,
        KEY_ICON,
        KEY_UUID,
        KEY_TYPE
    ),
    "$KEY_ROWID = ?",
    arrayOf(id.toString()),
    null
)?.use {
    if (it.moveToFirst())
        Category(
            id = id,
            parentId = it.getLongOrNull(0),
            label = it.getString(1),
            color = it.getIntOrNull(2),
            icon = it.getString(3),
            uuid = it.getString(4),
            type = it.getInt(5).toByte()
        ) else null
}

@VisibleForTesting
fun Repository.deleteAllCategories() {
    contentResolver.delete(
        TransactionProvider.CATEGORIES_URI,
        "$KEY_ROWID != ${DatabaseConstants.SPLIT_CATID}",
        null
    )
}