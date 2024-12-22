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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlin.math.min

/**
 * Calculates a [ScrollbarState] driven by the changes in a [LazyListState].
 *
 * @param itemsAvailable the total amount of items available to scroll in the lazy list.
 * */
@Composable
fun LazyListState.scrollbarState(
    itemsAvailable: Int,
): ScrollbarState {
    val state = remember { ScrollbarState() }
    LaunchedEffect(this, itemsAvailable) {
        snapshotFlow {
            if (itemsAvailable == 0) return@snapshotFlow null

            val visibleItemsInfo = layoutInfo.visibleItemsInfo

            if (visibleItemsInfo.isEmpty()) return@snapshotFlow null

            val firstIndex = min(
                a = interpolateFirstItemIndex(),
                b = itemsAvailable.toFloat(),
            )
            if (firstIndex.isNaN()) return@snapshotFlow null

            val itemsVisible = visibleItemsInfo.floatSumOf { itemInfo ->
                itemVisibilityPercentage(
                    itemSize = itemInfo.size,
                    itemStartOffset = itemInfo.offset,
                    viewportStartOffset = layoutInfo.viewportStartOffset,
                    viewportEndOffset = layoutInfo.viewportEndOffset,
                )
            }

            val thumbTravelPercent = min(
                a = firstIndex / itemsAvailable,
                b = 1f,
            )
            val thumbSizePercent = min(
                a = itemsVisible / itemsAvailable,
                b = 1f,
            )
            scrollbarStateValue(
                thumbSizePercent = thumbSizePercent,
                thumbMovedPercent = when {
                    layoutInfo.reverseLayout -> 1f - thumbTravelPercent
                    else -> thumbTravelPercent
                },
            )
        }
            .filterNotNull()
            .distinctUntilChanged()
            .collect { state.onScroll(it) }
    }
    return state
}

private inline fun <T> List<T>.floatSumOf(selector: (T) -> Float): Float =
    fold(initial = 0f) { accumulator, listItem -> accumulator + selector(listItem) }
