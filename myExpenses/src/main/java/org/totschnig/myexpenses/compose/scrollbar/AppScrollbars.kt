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

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.Orientation.Horizontal
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.InteractionSource
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.TEST_TAG_LIST
import org.totschnig.myexpenses.compose.simpleStickyHeader
import timber.log.Timber

/**
 * The time period for showing the scrollbar thumb after interacting with it, before it fades away
 */
private const val SCROLLBAR_INACTIVE_TO_DORMANT_TIME_IN_MS = 2_000L

@Composable
fun LazyColumnWithScrollbarAndBottomPadding(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    fastScroll: Boolean = false,
    itemsAvailable: Int,
    groupCount: Int = 0,
    withFab: Boolean = true,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    testTag: String = TEST_TAG_LIST,
    content: LazyListScope.() -> Unit,
) {
    LazyColumnWithScrollbar(
        modifier = modifier,
        state = state,
        fastScroll = fastScroll,
        itemsAvailable = itemsAvailable,
        groupCount = groupCount,
        contentPadding = PaddingValues(
            bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() +
                    if (withFab)
                        dimensionResource(R.dimen.fab_related_bottom_padding) else 0.dp
        ),
        verticalArrangement = verticalArrangement,
        testTag = testTag,
        content = content
    )
}

@Composable
fun LazyColumnWithScrollbar(
    modifier: Modifier = Modifier,
    state: LazyListState = rememberLazyListState(),
    fastScroll: Boolean = false,
    itemsAvailable: Int,
    groupCount: Int = 0,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    testTag: String = TEST_TAG_LIST,
    content: LazyListScope.() -> Unit,
) {
    val totalItems = itemsAvailable + groupCount
    Box(modifier = modifier) {
        LazyColumn(
            modifier = Modifier
                .testTag(testTag)
                .semantics {
                    collectionInfo = CollectionInfo(itemsAvailable, 1)
                },
            state = state,
            verticalArrangement = verticalArrangement,
            contentPadding = contentPadding,
            content = content
        )
        val scrollbarState = state.scrollbarState(totalItems)
        Timber.d("thumbSizePercent %f", scrollbarState.thumbSizePercent)
        if (scrollbarState.thumbSizePercent < 1f) {
            if (fastScroll) {
                state.DraggableScrollbar(
                    modifier = Modifier
                        .fillMaxHeight()
                        .windowInsetsPadding(WindowInsets.systemBars)
                        .padding(horizontal = 2.dp)
                        .align(Alignment.CenterEnd),
                    state = scrollbarState,
                    orientation = Vertical,
                    onThumbMoved = state.rememberDraggableScroller(totalItems)
                )
            } else {
                state.DecorativeScrollbar(
                    modifier = Modifier
                        .fillMaxHeight()
                        .align(Alignment.CenterEnd),
                    state = scrollbarState,
                    orientation = Vertical
                )
            }
        }
    }
}


/**
 * A simple [Scrollbar].
 * Its thumb disappears when the scrolling container is dormant.
 * @param modifier a [Modifier] for the [Scrollbar]
 * @param state the driving state for the [Scrollbar]
 * @param orientation the orientation of the scrollbar
 */
@Composable
fun ScrollableState.DecorativeScrollbar(
    state: ScrollbarState,
    orientation: Orientation,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Scrollbar(
        modifier = modifier,
        orientation = orientation,
        interactionSource = interactionSource,
        state = state,
        thumb = {
            DecorativeScrollbarThumb(
                interactionSource = interactionSource,
                orientation = orientation,
            )
        },
    )
}

/**
 * A [Scrollbar] that allows for fast scrolling of content by dragging its thumb.
 * Its thumb disappears when the scrolling container is dormant.
 * @param modifier a [Modifier] for the [Scrollbar]
 * @param state the driving state for the [Scrollbar]
 * @param orientation the orientation of the scrollbar
 * @param onThumbMoved the fast scroll implementation
 */
@Composable
fun ScrollableState.DraggableScrollbar(
    state: ScrollbarState,
    orientation: Orientation,
    onThumbMoved: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Scrollbar(
        modifier = modifier,
        orientation = orientation,
        interactionSource = interactionSource,
        state = state,
        thumb = {
            DraggableScrollbarThumb(
                interactionSource = interactionSource,
                orientation = orientation,
            )
        },
        onThumbMoved = onThumbMoved,
    )
}

/**
 * A decorative scrollbar thumb used solely for communicating a user's position in a list.
 */
@Composable
private fun ScrollableState.DecorativeScrollbarThumb(
    interactionSource: InteractionSource,
    orientation: Orientation,
) {
    Box(
        modifier = Modifier
            .run {
                when (orientation) {
                    Vertical -> width(4.dp).fillMaxHeight()
                    Horizontal -> height(4.dp).fillMaxWidth()
                }
            }
            .scrollThumb(this, interactionSource),
    )
}

/**
 * A scrollbar thumb that is intended to also be a touch target for fast scrolling.
 */
@Composable
private fun ScrollableState.DraggableScrollbarThumb(
    interactionSource: InteractionSource,
    orientation: Orientation,
) {
    Box(
        modifier = Modifier
            .run {
                when (orientation) {
                    Vertical -> width(12.dp).fillMaxHeight()
                    Horizontal -> height(12.dp).fillMaxWidth()
                }
            }
            .scrollThumb(this, interactionSource),
    )
}


@Composable
private fun Modifier.scrollThumb(
    scrollableState: ScrollableState,
    interactionSource: InteractionSource,
): Modifier {
    val colorState = scrollbarThumbColor(scrollableState, interactionSource)
    return this then ScrollThumbElement { colorState.value }
}

private data class ScrollThumbElement(val colorProducer: ColorProducer) :
    ModifierNodeElement<ScrollThumbNode>() {
    override fun create(): ScrollThumbNode = ScrollThumbNode(colorProducer)
    override fun update(node: ScrollThumbNode) {
        node.colorProducer = colorProducer
        node.invalidateDraw()
    }
}

private class ScrollThumbNode(var colorProducer: ColorProducer) : DrawModifierNode,
    Modifier.Node() {
    private val shape = RoundedCornerShape(16.dp)

    // naive cache outline calculation if size is the same
    private var lastSize: Size? = null
    private var lastLayoutDirection: LayoutDirection? = null
    private var lastOutline: Outline? = null

    override fun ContentDrawScope.draw() {
        val color = colorProducer()
        val outline =
            if (size == lastSize && layoutDirection == lastLayoutDirection) {
                lastOutline!!
            } else {
                shape.createOutline(size, layoutDirection, this)
            }
        if (color != Color.Unspecified) drawOutline(outline, color = color)

        lastOutline = outline
        lastSize = size
        lastLayoutDirection = layoutDirection
    }
}

/**
 * The color of the scrollbar thumb as a function of its interaction state.
 * @param interactionSource source of interactions in the scrolling container
 */
@Composable
private fun scrollbarThumbColor(
    scrollableState: ScrollableState,
    interactionSource: InteractionSource,
): State<Color> {
    var state by remember { mutableStateOf(ThumbState.Dormant) }
    val pressed by interactionSource.collectIsPressedAsState()
    val hovered by interactionSource.collectIsHoveredAsState()
    val dragged by interactionSource.collectIsDraggedAsState()
    val active = (scrollableState.canScrollForward || scrollableState.canScrollBackward) &&
            (pressed || hovered || dragged || scrollableState.isScrollInProgress)

    val color = animateColorAsState(
        targetValue = when (state) {
            ThumbState.Active -> MaterialTheme.colorScheme.onSurface.copy(0.5f)
            ThumbState.Inactive -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.2f)
            ThumbState.Dormant -> Color.Transparent
        },
        animationSpec = SpringSpec(
            stiffness = Spring.StiffnessLow,
        ),
        label = "Scrollbar thumb color",
    )
    LaunchedEffect(active) {
        when (active) {
            true -> state = ThumbState.Active
            false -> if (state == ThumbState.Active) {
                state = ThumbState.Inactive
                delay(SCROLLBAR_INACTIVE_TO_DORMANT_TIME_IN_MS)
                state = ThumbState.Dormant
            }
        }
    }

    return color
}

private enum class ThumbState {
    Active,
    Inactive,
    Dormant,
}

@Preview
@Composable
fun ListWithScrollbar() {
    val sections = 10
    val itemsAvailable = 10
    val withStickyHeaders = false
    val totalItems = sections * (itemsAvailable + if (withStickyHeaders) 1 else 0)
    AppTheme {
        LazyColumnWithScrollbar(fastScroll = true, itemsAvailable = totalItems) {
            repeat(sections) { section ->
                if (withStickyHeaders) {
                    simpleStickyHeader("section $section")
                }
                items(itemsAvailable) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "A long text message $section/$it"
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun ShouldNotFillMaxSize() {
    val sections = 3
    val itemsAvailable = 3
    val totalItems = sections * (itemsAvailable + 1)
    AppTheme {
        LazyColumnWithScrollbar(
            modifier = Modifier.background(color = Color.Red),
            itemsAvailable = totalItems
        ) {
            repeat(sections) { section ->
                simpleStickyHeader("section $section")
                items(itemsAvailable) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = "A long text message $section/$it"
                    )
                }
            }
        }
    }
}