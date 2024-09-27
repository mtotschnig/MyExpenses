package org.totschnig.myexpenses.db2

import org.apache.commons.text.translate.UnicodeUnescaper
import org.totschnig.myexpenses.model2.Category

object CategoryHelper {
    private val unicodeUnescaper = UnicodeUnescaper()
    private var countInserted = 0

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
        stripQifCategoryClass: Boolean,
        typeFlags: Byte = FLAG_NEUTRAL
    ): Int {
        countInserted = 0
        insertCategory(
            repository,
            parse(if (stripQifCategoryClass) stripCategoryClass(name) else name),
            categoryToId,
            typeFlags
        )
        return countInserted
    }

    private fun stripCategoryClass(name: String): String {
        val i = name.indexOf('/')
        return if (i != -1) {
            name.substring(0, i)
        } else name
    }

    private fun parse(name: String) = name.split(":".toRegex())

    private fun insertCategory(
        repository: Repository,
        name: List<String>,
        categoryToId: MutableMap<String, Long>,
        typeFlags: Byte = FLAG_NEUTRAL
    ) {
        var parentId: Long? = null
        var path = ""
        name.forEach {
            if (path.isNotEmpty()) path += ":"
            path += it
            var id = categoryToId[path]
            if (id == null) {
                id = maybeWriteCategory(repository, it, parentId, typeFlags)
                if (id != -1L) categoryToId[path] = id
            }
            if (id == -1L) {
                return
            } else {
                parentId = id
            }
        }
    }

    private fun maybeWriteCategory(
        repository: Repository,
        name: String,
        parentId: Long?,
        typeFlags: Byte = FLAG_NEUTRAL
    ): Long {
        val unescaped = unicodeUnescaper.translate(name)
        var id = repository.findCategory(unescaped, parentId)
        if (id == -1L) {
            id = repository.saveCategory(
                Category(
                    label = unescaped,
                    parentId = parentId,
                    type = typeFlags
                )
            ) ?: -1
            if (id != -1L) countInserted++
        }
        return id
    }

}