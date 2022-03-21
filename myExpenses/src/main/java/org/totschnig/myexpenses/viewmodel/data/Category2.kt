package org.totschnig.myexpenses.viewmodel.data

import android.os.Parcelable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.parcelize.Parcelize

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
    val icon: String? = null
) : Parcelable {

    fun flatten(): List<Category2> =  buildList {
        add(this@Category2)
        addAll(children.flatMap { it.flatten() })
    }

    fun pruneNonMatching(): Category2? {
        val prunedChildren = children.mapNotNull { it.pruneNonMatching() }
        return if (isMatching || prunedChildren.isNotEmpty()) {
            this.copy(children = prunedChildren)
        } else null
    }

    fun recursiveUnselectChildren(selectionState: SnapshotStateList<Long>) {
        children.forEach {
            selectionState.remove(it.id)
            it.recursiveUnselectChildren(selectionState)
        }
    }

    companion object {
        val EMPTY = Category2(label = "EMPTY")
    }
}