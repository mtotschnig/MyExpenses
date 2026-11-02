package org.totschnig.myexpenses.designsystem

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.dp

/**
 * Distances used throughout the new UI
 */
object Spacing {
    /** Between the edge of the screen and cards */
    val screen = 16.dp

    /** Inside of rows and cards */
    val content = 16.dp

    /** Above a section title or header */
    val section = 24.dp

    /** Between small elements, e.g. text and badge */
    val small = 8.dp
}

/**
 * Shapes used throughout the new UI
 */
object Shapes {
    val card = RoundedCornerShape(16.dp)
    val tile = RoundedCornerShape(12.dp)
    val chip = RoundedCornerShape(8.dp)
    val badge = RoundedCornerShape(4.dp)

    /** Fully rounded ends, e.g. a search field */
    val pill = RoundedCornerShape(50)

    /**
     * Items of a group look like one card: only the outer corners are rounded
     */
    fun groupItem(position: GroupPosition) = RoundedCornerShape(
        topStart = if (position.isFirst) 16.dp else 0.dp,
        topEnd = if (position.isFirst) 16.dp else 0.dp,
        bottomStart = if (position.isLast) 16.dp else 0.dp,
        bottomEnd = if (position.isLast) 16.dp else 0.dp,
    )
}

/**
 * Where an item is in a group of items that look like one card
 */
@Immutable
data class GroupPosition(val isFirst: Boolean, val isLast: Boolean) {
    companion object {
        val Single = GroupPosition(isFirst = true, isLast = true)

        fun of(index: Int, count: Int) = GroupPosition(isFirst = index == 0, isLast = index == count - 1)
    }
}
