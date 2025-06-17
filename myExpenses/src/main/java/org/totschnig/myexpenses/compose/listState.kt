package org.totschnig.myexpenses.compose

import androidx.compose.foundation.lazy.LazyListState
import timber.log.Timber

suspend fun LazyListState.scrollToPositionIfNotVisible(position: Int) {
    Timber.d("scrollToPositionIfNotVisible: $position")
    val currentlyVisibleItemsInfo = layoutInfo.visibleItemsInfo
    if (!currentlyVisibleItemsInfo.any { it.index == position }) {
        animateScrollToItem(position)
    } else {
        val itemInfo = currentlyVisibleItemsInfo.find { it.index == position }
        if (itemInfo != null) {
            val viewportStartOffset = layoutInfo.viewportStartOffset
            val viewportEndOffset = layoutInfo.viewportEndOffset

            val itemStartOffset = itemInfo.offset
            val itemEndOffset = itemInfo.offset + itemInfo.size

            val isFullyVisible = itemStartOffset >= viewportStartOffset && itemEndOffset <= viewportEndOffset

            if (!isFullyVisible) {
                animateScrollToItem(index = position)
            }
        }
    }
}