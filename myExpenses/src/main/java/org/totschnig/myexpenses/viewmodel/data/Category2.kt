package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import kotlin.math.absoluteValue

@Immutable
@Parcelize
data class Category2(
    val id: Long = 0,
    val parentId: Long? = null,
    val level: Int = 0,
    val label: String,
    val path: String = label,
    val children: List<Category2> = emptyList(),
    val isMatching: Boolean = true,
    val color: Int? = null,
    val icon: String? = null,
    val sum: Long = 0L,
    val budget: Long = 0L
) : Parcelable {

    fun flatten(): List<Category2> = buildList {
        add(this@Category2)
        addAll(children.flatMap { it.flatten() })
    }

    fun pruneNonMatching(_criteria: ((Category2) -> Boolean)? = null): Category2? {
        val criteria = _criteria ?: { it.isMatching }
        val prunedChildren = children.mapNotNull { it.pruneNonMatching(criteria) }
        return if (id == 0L || criteria(this) || prunedChildren.isNotEmpty()) {
            copy(children = prunedChildren)
        } else null
    }

    fun sortChildrenBySumRecursive(): Category2 = if (children.isEmpty()) this else
        copy(children = children.sortedByDescending { it.aggregateSum.absoluteValue }.map {
            it.sortChildrenBySumRecursive()
        })

    fun withSubColors(subColorProvider: (Int) -> List<Int>): Category2 =
        if (children.isEmpty()) this else
            copy(children = (if (color == null) children else {
                val subColors = subColorProvider(color)
                children.mapIndexed { index, category -> category.copy(color = subColors[index % subColors.size]) }
            }).map { it.withSubColors(subColorProvider) })


    fun sortChildrenByBudgetRecursive(): Category2 = if (children.isEmpty()) this else
        copy(children = children.sortedByDescending { it.budget }.map {
            it.sortChildrenByBudgetRecursive()
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

    companion object {
        val LOADING = Category2(label = "EMPTY")
    }
}