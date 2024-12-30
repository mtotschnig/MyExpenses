/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.totschnig.myexpenses.compose.scrollbar

import androidx.compose.foundation.lazy.LazyListState
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import timber.log.Timber
import kotlin.math.abs

/**
 * Linearly interpolates the index for the first item for smooth scrollbar
 * progression taking sticky header into account if exists.
 * @return a [Float] in the range [firstItemPosition..firstItemPosition+1)
 * */
internal fun LazyListState.interpolateFirstItemIndex(): Float {
    val visibleItems = layoutInfo.visibleItemsInfo
    if (visibleItems.isEmpty()) return 0f

    val header = visibleItems.firstOrNull {
        it.contentType == STICKY_HEADER_CONTENT_TYPE
    }

    val firstItem = header?.let {
        visibleItems.firstOrNull { it.offset + it.size > header.size }.also {
            if (it == null) {
                Timber.w("visibleItems : %s", visibleItems.joinToString { "${it.offset}/${it.size}" })
                CrashHandler.report(IllegalStateException("No item found"))
            }
        }
    } ?: visibleItems.first()

    val firstItemIndex = firstItem.index

    if (firstItemIndex < 0) return Float.NaN

    val firstItemSize = firstItem.size
    if (firstItemSize == 0) return Float.NaN

    val itemOffset = (firstItem.offset - (header?.size ?: 0)).toFloat()
    val offsetPercentage = (abs(itemOffset) / firstItemSize)

    return firstItemIndex + offsetPercentage
}

/**
 * Returns the percentage of an item that is currently visible in the view port.
 * @param itemSize the size of the item
 * @param itemStartOffset the start offset of the item relative to the view port start
 * @param viewportStartOffset the start offset of the view port
 * @param viewportEndOffset the end offset of the view port
 */
internal fun itemVisibilityPercentage(
    itemSize: Int,
    itemStartOffset: Int,
    viewportStartOffset: Int,
    viewportEndOffset: Int,
): Float {
    if (itemSize == 0) return 0f
    val itemEnd = itemStartOffset + itemSize
    val startOffset = when {
        itemStartOffset > viewportStartOffset -> 0
        else -> abs(abs(viewportStartOffset) - abs(itemStartOffset))
    }
    val endOffset = when {
        itemEnd < viewportEndOffset -> 0
        else -> abs(abs(itemEnd) - abs(viewportEndOffset))
    }
    val size = itemSize.toFloat()
    return (size - startOffset - endOffset) / size
}
