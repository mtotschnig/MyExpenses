package org.totschnig.myexpenses.db2

import android.content.ContentUris
import org.apache.commons.lang3.StringUtils
import org.totschnig.myexpenses.viewmodel.data.Category

object CategoryHelper {
    var countInserted = 0
    /**
     * inserts the category to the database if needed
     * @param categoryToId a map which caches the relation between the category name and the database
     * id, both the root and the child category are placed in this map
     * @return the number of new elements added to the database
     */
    @Synchronized
    fun insert(
        repository: Repository,
        name: String,
        categoryToId: MutableMap<String, Long>,
        stripQifCategoryClass: Boolean
    ): Int {
        countInserted = 0
        val name: String = if (stripQifCategoryClass) stripCategoryClass(name) else name
        insertCategory(reduceToTwoLevels(name), categoryToId, repository)
        return countInserted
    }

    private fun stripCategoryClass(name: String): String {
        val i = name.indexOf('/')
        return if (i != -1) {
            name.substring(0, i)
        } else name
    }

    private fun reduceToTwoLevels(name: String): String {
        if (StringUtils.countMatches(name, ':') > 1) {
            val parts = name.split(":".toRegex()).toTypedArray()
            return parts[0] + ":" + parts[1]
        }
        return name
    }

    private fun insertCategory(
        name: String,
        categoryToId: MutableMap<String, Long>,
        repository: Repository
    ) {
        if (isChildCategory(name)) {
            insertChildCategory(repository, name, categoryToId)
        } else {
            insertRootCategory(repository, name, categoryToId)
        }
    }

    private fun isChildCategory(name: String): Boolean {
        return name.contains(":")
    }

    private fun insertRootCategory(
        repository: Repository,
        name: String,
        categoryToId: MutableMap<String, Long>
    ): Long {
        var id = categoryToId[name]
        if (id == null) {
            id = maybeWriteCategory(repository, name, null)
            if (id != -1L) categoryToId[name] = id
        }
        return id
    }

    private fun maybeWriteCategory(repository: Repository, name: String, parentId: Long?): Long {
        var id = repository.findCategory(name, parentId)
        if (id == -1L) {
            id = repository.saveCategory(Category(label = name, parentId = parentId))
                ?.let { ContentUris.parseId(it) } ?: -1
            if (id != -1L) countInserted++
        }
        return id
    }

    private fun insertChildCategory(
        repository: Repository,
        name: String,
        categoryToId: MutableMap<String, Long>
    ): Long? {
        var id = categoryToId[name]
        if (id == null) {
            val i = name.lastIndexOf(':')
            val parentCategoryName = name.substring(0, i)
            val childCategoryName = name.substring(i + 1)
            val main = insertRootCategory(repository, parentCategoryName, categoryToId)
            if (main != -1L) {
                id = maybeWriteCategory(repository, childCategoryName, main)
                if (id != -1L) categoryToId[name] = id
            }
        }
        return id
    }
}