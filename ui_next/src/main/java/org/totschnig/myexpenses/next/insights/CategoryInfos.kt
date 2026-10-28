package org.totschnig.myexpenses.next.insights

import org.totschnig.myexpenses.db2.FLAG_EXPENSE
import org.totschnig.myexpenses.db2.FLAG_INCOME
import org.totschnig.myexpenses.next.core.model.ledger.CategoryInfo

/** A category as stored, see [categoryInfos] */
class RawCategory(val id: Long, val parentId: Long?, val label: String, val type: Int)

/** Gives the categories their paths and takes their direction from their main category */
fun categoryInfos(raw: List<RawCategory>): List<CategoryInfo> {
    val byId = raw.associateBy { it.id }
    fun pathOf(category: RawCategory): String =
        category.parentId?.let(byId::get)?.let { "${pathOf(it)} > ${category.label}" } ?: category.label
    fun rootOf(category: RawCategory): RawCategory = category.parentId?.let(byId::get)?.let(::rootOf) ?: category
    return raw.map { category ->
        val root = rootOf(category)
        // The type is set on main categories
        val type = root.type
        CategoryInfo(
            id = category.id,
            label = category.label,
            path = pathOf(category),
            isExpense = type == 0 || type and FLAG_EXPENSE.toInt() != 0,
            isIncome = type == 0 || type and FLAG_INCOME.toInt() != 0,
            mainId = root.id,
            parentId = category.parentId?.takeIf { it in byId }
        )
    }
}
