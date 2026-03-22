package org.totschnig.myexpenses.compose.main

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

@Composable
fun rememberCollapsingTabRowState(): CollapsingTabRowState {
    return remember { CollapsingTabRowState() }
}

class CollapsingTabRowState {
    // Initialize with 0, will be set via onSizeChanged
    var maxHeightPx by mutableStateOf<Float?>(null)

    var offsetPx by mutableFloatStateOf(0f)
        private set

    // The current dynamic height
    val heightPx: Float?
        get() = maxHeightPx?.let {
            (it + offsetPx).coerceIn(0f, it)
        }

    val nestedScrollConnection = object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ) = if (maxHeightPx == null || maxHeightPx == 0f) Offset.Zero else {

            val delta = available.y
            val newOffset = (offsetPx + delta).coerceIn(-(maxHeightPx ?: 0f), 0f)
            val consumed = newOffset - offsetPx
            offsetPx = newOffset
            Offset(0f, consumed)
        }
    }
}