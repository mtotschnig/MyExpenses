package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.db2.FLAG_NEUTRAL
import java.io.Serializable
import kotlin.math.absoluteValue

@Immutable
@Parcelize
data class Category(
    val id: Long = 0,
    val parentId: Long? = null,
    val level: Int = 0,
    val label: String = "",
    val path: String = label,
    val children: List<Category> = emptyList(),
    val isMatching: Boolean = false,
    val color: Int? = null,
    val icon: String? = null,
    val sum: Long = 0L,
    val budget: BudgetAllocation = BudgetAllocation.EMPTY,
    val uuid: String? = null,
    /**
     * [org.totschnig.myexpenses.db2.FLAG_EXPENSE]
     * [org.totschnig.myexpenses.db2.FLAG_INCOME]
     * [org.totschnig.myexpenses.db2.FLAG_NEUTRAL]
     * [org.totschnig.myexpenses.db2.FLAG_TRANSFER]
     */
    val typeFlags: Byte = FLAG_NEUTRAL
) : Parcelable, Serializable {

    fun flatten(): List<Category> = buildList {
        add(this@Category)
        addAll(children.flatMap { it.flatten() })
    }

    fun getExpandedForSelected(selected: List<Long>): List<Long> =
        buildList {
            val expandedChildren = children.filter { !selected.contains(it.id) }.flatMap {
                it.getExpandedForSelected(selected)
            }
            if (id != 0L && (children.any { selected.contains(it.id) } || expandedChildren.isNotEmpty())) {
                add(id)
            }
            addAll(expandedChildren)
        }

    fun pruneNonMatching() = pruneByCriterion { it.isMatching }

    fun pruneByCriterion(criterion: ((Category) -> Boolean)?): Category? {
        if (criterion == null) return this
        val prunedChildren = children.mapNotNull { it.pruneByCriterion(criterion) }
        return if (criterion(this) || prunedChildren.isNotEmpty()) {
            copy(children = prunedChildren)
        } else null
    }

    fun withSubColors(subColorProvider: (Int) -> List<Int>): Category =
        if (children.isEmpty()) this else
            copy(children = (if (color == null) children else {
                val subColors = subColorProvider(color)
                children.mapIndexed { index, category -> category.copy(color = subColors[index % subColors.size]) }
            }).map { it.withSubColors(subColorProvider) })


    fun sortChildrenByBudgetRecursive() = sortChildrenRecursive { it.budget.totalAllocated }

    fun sortChildrenBySumRecursive() = sortChildrenRecursive { it.aggregateSum.absoluteValue }

    fun sortChildrenByAvailableRecursive() = sortChildrenRecursive {
        it.budget.totalAllocated + it.aggregateSum
    }

    private fun sortChildrenRecursive(selector: (Category) -> Long): Category =
        if (children.isEmpty()) this else
            copy(children = children.sortedByDescending(selector).map {
                it.sortChildrenRecursive(selector)
            })

    fun recursiveUnselectChildren(selectionState: SnapshotStateList<Long>) {
        children.forEach {
            selectionState.remove(it.id)
            it.recursiveUnselectChildren(selectionState)
        }
    }

    /**
     * at the moment, the root category gets initialized with the total spent as sum
     * this could be refactored by getting unmapped expenses as part of the category tree
     */
    @IgnoredOnParcel
    val aggregateSum: Long
        get() = sum + if (level == 0) 0 else children.sumOf { it.aggregateSum }

    val hasRolloverNext: Boolean
        get() = budget.rollOverNext != 0L || children.any { it.budget.rollOverNext != 0L }

    companion object {
        val LOADING = Category(label = "LOADING")
        val EMPTY = Category(label = "EMPTY")
    }
}