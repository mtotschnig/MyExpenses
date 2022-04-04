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

    fun flatten(): List<Category2> =  buildList {
        add(this@Category2)
        addAll(children.flatMap { it.flatten() })
    }

    fun pruneNonMatching(_criteria: ((Category2) -> Boolean)? = null): Category2? {
        val criteria = _criteria ?: { it.isMatching }
        val prunedChildren = children.mapNotNull { it.pruneNonMatching(criteria) }
        return if (id == 0L || criteria(this) || prunedChildren.isNotEmpty()) {
            this.copy(children = prunedChildren)
        } else null
    }

    fun sortChildrenBySum(): Category2 = if (children.isEmpty()) this else
        this.copy(children = children.sortedByDescending { it.aggregateSum.absoluteValue })

    fun recursiveUnselectChildren(selectionState: SnapshotStateList<Long>) {
        children.forEach {
            selectionState.remove(it.id)
            it.recursiveUnselectChildren(selectionState)
        }
    }

    @IgnoredOnParcel
    val aggregateSum: Long by lazy { sum + children.sumOf { it.aggregateSum } }

    companion object {
        val EMPTY = Category2(label = "EMPTY")
    }
}