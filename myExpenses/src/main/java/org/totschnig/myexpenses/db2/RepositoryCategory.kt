package org.totschnig.myexpenses.db2

import android.content.ContentProviderOperation
import android.content.ContentProviderOperation.newDelete
import android.content.ContentProviderOperation.newInsert
import android.content.ContentProviderOperation.newUpdate
import android.content.ContentUris
import android.content.ContentValues
import android.database.sqlite.SQLiteConstraintException
import android.net.Uri
import androidx.annotation.VisibleForTesting
import androidx.core.database.getIntOrNull
import androidx.core.database.getLongOrNull
import androidx.core.database.getStringOrNull
import arrow.core.Tuple5
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.json.CategoryExport
import org.totschnig.myexpenses.sync.json.ICategoryInfo
import org.totschnig.myexpenses.util.Utils
import org.totschnig.myexpenses.viewmodel.data.Category
import java.io.IOException
import java.util.ArrayList
import java.util.UUID

fun Repository.saveCategory(category: Category): Uri? {
    val initialValues = ContentValues().apply {
        put(DatabaseConstants.KEY_LABEL, category.label.trim())
        put(DatabaseConstants.KEY_LABEL_NORMALIZED, Utils.normalize(category.label))
        category.color.takeIf { it != 0 }?.let {
            put(DatabaseConstants.KEY_COLOR, it)
        }
        put(DatabaseConstants.KEY_ICON, category.icon)
        if (category.id == 0L) {
            put(DatabaseConstants.KEY_PARENTID, category.parentId)
        }
    }
    return try {
        if (category.id == 0L) {
            initialValues.put(DatabaseConstants.KEY_UUID, category.uuid ?: UUID.randomUUID().toString())
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
            put(DatabaseConstants.KEY_PARENTID, target)
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
        put(DatabaseConstants.KEY_COLOR, color)
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
        "${DatabaseConstants.KEY_PARENTID} is null" to emptyArray()
    } else {
        "${DatabaseConstants.KEY_PARENTID} = ?" to arrayOf(parentId.toString())
    }
    return contentResolver.query(
        TransactionProvider.CATEGORIES_URI,
        arrayOf(DatabaseConstants.KEY_ROWID),
        "${DatabaseConstants.KEY_LABEL} = ? AND $parentSelection",
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
            DatabaseConstants.KEY_ROWID,
            DatabaseConstants.KEY_LABEL,
            DatabaseConstants.KEY_PARENTID,
            DatabaseConstants.KEY_ICON,
            DatabaseConstants.KEY_COLOR
        ),
        Array(uuids.size) { "instr(${DatabaseConstants.KEY_UUID}, ?) > 0" }.joinToString(" OR "),
        uuids.toTypedArray(),
        null
    )?.use {
        if (it.moveToFirst()) {
            Tuple5(
                it.getLong(0),
                it.getString(1),
                it.getLongOrNull(2),
                it.getStringOrNull(3),
                it.getIntOrNull(4)
            )
        } else null
    }

    val (parentSelection, parentSelectionArgs) = if (parentId == null) {
        "${DatabaseConstants.KEY_PARENTID} is null" to emptyArray()
    } else {
        "${DatabaseConstants.KEY_PARENTID} = ?" to arrayOf(parentId.toString())
    }

    val target = contentResolver.query(
        TransactionProvider.CATEGORIES_URI,
        arrayOf(DatabaseConstants.KEY_ROWID, DatabaseConstants.KEY_UUID),
        "${DatabaseConstants.KEY_LABEL} = ? AND $parentSelection",
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
                    put(DatabaseConstants.KEY_LABEL, categoryInfo.label)
                }
                if (sourceBasedOnUuid.third != parentId) {
                    put(DatabaseConstants.KEY_PARENTID, parentId)
                }
                if (sourceBasedOnUuid.fourth != categoryInfo.icon) {
                    put(DatabaseConstants.KEY_ICON, categoryInfo.icon)
                }
                if (sourceBasedOnUuid.fifth != categoryInfo.color) {
                    put(DatabaseConstants.KEY_COLOR, categoryInfo.color)
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
                put(DatabaseConstants.KEY_CATID, target.first)
            }
            val selection = "${DatabaseConstants.KEY_CATID} = ?"
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

        operations.add(ContentProviderOperation.newUpdate(ContentUris.withAppendedId(TransactionProvider.CATEGORIES_URI, target.first))
            .withValues(
                ContentValues(3).apply {
                    put(DatabaseConstants.KEY_UUID, newUuids)
                    put(DatabaseConstants.KEY_ICON, categoryInfo.icon)
                    put(DatabaseConstants.KEY_COLOR, categoryInfo.color)
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
            color = categoryInfo.color
        )
    )?.let { ContentUris.parseId(it) to true }
        ?: throw IOException("Saving category failed")
}

@VisibleForTesting
fun Repository.loadCategory(id: Long): Category? = contentResolver.query(
    TransactionProvider.CATEGORIES_URI,
    arrayOf(
        DatabaseConstants.KEY_PARENTID,
        DatabaseConstants.KEY_LABEL,
        DatabaseConstants.KEY_COLOR,
        DatabaseConstants.KEY_ICON,
        DatabaseConstants.KEY_UUID
    ),
    "${DatabaseConstants.KEY_ROWID} = ?",
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
            uuid = it.getString(4)
        ) else null
}