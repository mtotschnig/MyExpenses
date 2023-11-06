package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.newDelete
import android.content.ContentProviderOperation.newUpdate
import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import arrow.core.Tuple6
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_CATID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_COLOR
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ICON
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_LABEL_NORMALIZED
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_PARENTID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_ROWID
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_TYPE
import org.totschnig.myexpenses.provider.DatabaseConstants.KEY_UUID
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.json.CategoryExport
import org.totschnig.myexpenses.sync.json.ICategoryInfo
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.data.Category
import java.io.IOException
import java.util.UUID

const val FLAG_TRANSFER: UByte = 0u
const val FLAG_EXPENSE: UByte = 1u
const val FLAG_INCOME: UByte = 2u
val FLAG_NEUTRAL = FLAG_EXPENSE or FLAG_INCOME

fun Repository.saveCategory(category: Category): Uri? {
    val initialValues = ContentValues().apply {
        put(KEY_LABEL, category.label.trim())
        put(KEY_LABEL_NORMALIZED, Utils.normalize(category.label))
        category.color.takeIf { it != 0 }?.let {
            put(KEY_COLOR, it)
        }
        put(KEY_ICON, category.icon)
        if (category.id == 0L) {
            put(KEY_PARENTID, category.parentId)
        }
        if (category.parentId == null) {
            put(KEY_TYPE, (category.typeFlags ?: FLAG_NEUTRAL).toInt())
        }
    }
    return try {
        if (category.id == 0L) {
            initialValues.put(KEY_UUID, category.uuid ?: UUID.randomUUID().toString())
            contentResolver.insert(TransactionProvider.CATEGORIES_URI, initialValues)
        } else {
            TransactionProvider.CATEGORIES_URI.buildUpon().appendPath(category.id.toString()).build().let {
                if (contentResolver.update(it, initialValues, null, null) == 0)
                    null else it
            }
        }
    } catch (e: SQLiteConstraintException) {
        null
    }
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

fun Repository.ensureCategoryTree(categoryExport: CategoryExport, parentId: Long?): Int {
    check(categoryExport.uuid.isNotEmpty())
    val (nextParent, created) = ensureCategory(categoryExport, parentId)
    var count = if (created) 1 else 0
    categoryExport.children.forEach {
        count += ensureCategoryTree(it, nextParent)
    }
    return count
}

/**
 * 1 if the category with provided uuid exists,
 *  1.1 if target (a category with the provided label and parent) does not exist: update label, parent, icon, color and return category
 *  1.2 if target exists: move all usages of the source category into target category; delete source category and continue with 2.1
 * 2. otherwise
 * 2.1 if target exists, (update icon), append uuid and return it
 * 2.2 otherwise create category with label and uuid and return it
 *
 * @return pair of category id and boolean that is true if a new category has been created
 */
fun Repository.ensureCategory(categoryInfo: ICategoryInfo, parentId: Long?): Pair<Long, Boolean> {
    val stripped = categoryInfo.label.trim()
    val uuids = categoryInfo.uuid.split(Repository.UUID_SEPARATOR)

    val sourceBasedOnUuid = contentResolver.query(
        TransactionProvider.CATEGORIES_URI,
        arrayOf(
            KEY_ROWID,
            KEY_LABEL,
            KEY_PARENTID,
            KEY_ICON,
            KEY_COLOR,
            KEY_TYPE
        ),
        Array(uuids.size) { "instr($KEY_UUID, ?) > 0" }.joinToString(" OR "),
        uuids.toTypedArray(),
        null
    )?.use {
        if (it.moveToFirst()) {
            Tuple6(
                it.getLong(0),
                it.getString(1),
                it.getLongOrNull(2),
                it.getStringOrNull(3),
                it.getIntOrNull(4),
                it.getInt(5)
            )
        } else null
    }

    val (parentSelection, parentSelectionArgs) = if (parentId == null) {
        "$KEY_PARENTID is null" to emptyArray()
    } else {
        "$KEY_PARENTID = ?" to arrayOf(parentId.toString())
    }

    val target = contentResolver.query(
        TransactionProvider.CATEGORIES_URI,
        arrayOf(KEY_ROWID, KEY_UUID),
        "$KEY_LABEL = ? AND $parentSelection",
        arrayOf(stripped) + parentSelectionArgs,
        null
    )?.use {
        if (it.moveToFirst()) {
            it.getLong(0) to it.getString(1)
        } else null
    }

    val operations = ArrayList<ContentProviderOperation>()

    if (sourceBasedOnUuid != null) {
        if (target == null || target.first == sourceBasedOnUuid.first) {
            val categoryId = sourceBasedOnUuid.first
            val contentValues = ContentValues().apply {
                if (sourceBasedOnUuid.second != categoryInfo.label) {
                    put(KEY_LABEL, categoryInfo.label)
                }
                if (sourceBasedOnUuid.third != parentId) {
                    put(KEY_PARENTID, parentId)
                }
                if (sourceBasedOnUuid.fourth != categoryInfo.icon) {
                    put(KEY_ICON, categoryInfo.icon)
                }
                if (sourceBasedOnUuid.fifth != categoryInfo.color) {
                    put(KEY_COLOR, categoryInfo.color)
                }
                if (parentId == null && categoryInfo.type != null && sourceBasedOnUuid.sixth != categoryInfo.type) {
                    put(KEY_TYPE, categoryInfo.type)
                }
            }
            if (contentValues.size() > 0) {
                contentResolver.update(
                    ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, categoryId),
                    contentValues,
                    null, null
                )
            }
            return categoryId to false
        } else {
            val contentValues = ContentValues(1).apply {
                put(KEY_CATID, target.first)
            }
            val selection = "$KEY_CATID = ?"
            val selectionArgs = arrayOf(sourceBasedOnUuid.first.toString())
            operations.add(newUpdate(TransactionProvider.TRANSACTIONS_URI).withValues(contentValues).withSelection(selection, selectionArgs).build())
            operations.add(newUpdate(TransactionProvider.TEMPLATES_URI).withValues(contentValues).withSelection(selection, selectionArgs).build())
            operations.add(newUpdate(TransactionProvider.CHANGES_URI).withValues(contentValues).withSelection(selection, selectionArgs).build())
            operations.add(newUpdate(TransactionProvider.BUDGET_ALLOCATIONS_URI).withValues(contentValues).withSelection(selection, selectionArgs).build())
            operations.add(newDelete(ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, sourceBasedOnUuid.first)).build())
            //Ideally, we would also need to update filters
        }
    }

    if (target != null) {
        val newUuids = (uuids + target.second.split(Repository.UUID_SEPARATOR))
            .distinct()
            .joinToString(Repository.UUID_SEPARATOR)

        operations.add(
            newUpdate(ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, target.first))
            .withValues(
                ContentValues().apply {
                    put(KEY_UUID, newUuids)
                    put(KEY_ICON, categoryInfo.icon)
                    put(KEY_COLOR, categoryInfo.color)
                    if (parentId == null && categoryInfo.type != null) {
                        put(KEY_TYPE, categoryInfo.type)
                    }
                }
            ).build()
        )
        contentResolver.applyBatch(TransactionProvider.AUTHORITY, operations)

        return target.first to false
    }

    return saveCategory(
        Category(
            label = categoryInfo.label,
            parentId = parentId,
            icon = categoryInfo.icon,
            uuid = categoryInfo.uuid,
            color = categoryInfo.color,
            typeFlags = if (parentId == null) categoryInfo.type?.toUByte() ?: FLAG_NEUTRAL else null
        )
    )?.let { ContentUris.parseId(it) to true }
        ?: throw IOException("Saving category failed")
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
            typeFlags = it.getInt(5).toUByte()
        ) else null
}