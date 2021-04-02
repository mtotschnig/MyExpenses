package org.totschnig.myexpenses.viewmodel.data

import java.util.*

data class Category(val id: Long, val parentId: Long?, val label: String, val sum: Long?,
               val hasMappedBudgets: Boolean?, val color: Int, val budget: Long?, val icon: String?) {
    private val children: MutableList<Category> = ArrayList()
    fun addChild(child: Category) {
        check(child.parentId == id) { "Cannot accept child with wrong parent" }
        children.add(child)
    }

    fun hasChildren(): Boolean {
        return children.isNotEmpty()
    }

    val childCount: Int
        get() = children.size

    fun getChildAt(index: Int): Category {
        return children[index]
    }

    fun getChildren(): List<Category> = children
}