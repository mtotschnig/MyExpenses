package org.totschnig.myexpenses.designsystem

import android.view.View
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.roundToIntRect
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import kotlin.math.roundToInt

/**
 * A kind of thing a [MenuFab] creates
 *
 * @param iconTint e.g. the color of expenses, [Color.Unspecified] for the color of the text
 */
class FabMenuItem(
    val label: String,
    val icon: ImageVector,
    val iconTint: Color = Color.Unspecified,
    val onClick: () -> Unit,
)

/**
 * Floating button that creates something and says what, e.g. "+ Transaction". A tap opens the
 * kinds that can be created as labelled items above it, the first one nearest to the thumb.
 * If [onClick] is given, e.g. for a kind fixed in the settings, a tap creates it directly and
 * the items open on a long press.
 *
 * @param isExtended shows [label] next to the icon, usually false while a list is scrolled down
 * @param menuLabel announces that a long press opens the items, when there is [onClick]
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MenuFab(
    label: String,
    icon: ImageVector,
    items: List<FabMenuItem>,
    menuLabel: String,
    closeLabel: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isExtended: Boolean = true,
    containerColor: Color = MaterialTheme.colorScheme.primaryContainer,
    contentColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
) {
    var isOpen by rememberSaveable { mutableStateOf(false) }
    // The box is the anchor of the popup with the items
    // Where the button is, to place the items and a button for closing above the dimmed page
    val hostView = LocalView.current
    var anchorOnScreen by remember { mutableStateOf(Rect.Zero) }
    Box(modifier.onGloballyPositioned { anchorOnScreen = it.boundsOnScreen(hostView) }) {
        MenuFabButton(
            label = label,
            icon = icon,
            isOpen = isOpen,
            onOpenChange = { isOpen = it },
            menuLabel = menuLabel,
            closeLabel = closeLabel,
            onClick = onClick,
            isExtended = isExtended,
            containerColor = containerColor,
            contentColor = contentColor
        )
        if (isOpen) {
            MenuFabPopup(
                items = items,
                anchorOnScreen = anchorOnScreen,
                closeLabel = closeLabel,
                onClose = { isOpen = false },
                containerColor = containerColor,
                contentColor = contentColor
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MenuFabButton(
    label: String,
    icon: ImageVector,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    menuLabel: String,
    closeLabel: String,
    onClick: (() -> Unit)?,
    isExtended: Boolean,
    containerColor: Color,
    contentColor: Color,
) {
    val haptic = LocalHapticFeedback.current
    Surface(
        shape = FloatingActionButtonDefaults.extendedFabShape,
        color = containerColor,
        contentColor = contentColor,
        shadowElevation = 6.dp
    ) {
        Row(
            modifier = Modifier
                .combinedClickable(
                    role = Role.Button,
                    onClickLabel = if (isOpen) closeLabel else if (onClick == null) menuLabel else null,
                    onLongClickLabel = menuLabel.takeIf { onClick != null && !isOpen },
                    onLongClick = if (onClick != null) {
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            onOpenChange(!isOpen)
                        }
                    } else null,
                    onClick = {
                        when {
                            isOpen -> onOpenChange(false)
                            onClick != null -> onClick()
                            else -> onOpenChange(true)
                        }
                    }
                )
                .semantics { contentDescription = if (isOpen) closeLabel else label }
                .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(if (isOpen) Icons.Default.Close else icon, contentDescription = null)
            AnimatedVisibility(
                visible = isExtended && !isOpen,
                enter = expandHorizontally(),
                exit = shrinkHorizontally()
            ) {
                Row {
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun MenuFabPopup(
    items: List<FabMenuItem>,
    anchorOnScreen: Rect,
    closeLabel: String,
    onClose: () -> Unit,
    containerColor: Color,
    contentColor: Color,
) {
    Popup(
        popupPositionProvider = WholeWindow,
        onDismissRequest = onClose,
        properties = PopupProperties(focusable = true)
    ) {
        // The page is dimmed, a tap on it closes the menu like the button below the items.
        // The popup need not start where the page starts, e.g. below the status bar.
        val popupView = LocalView.current
        var popupOnScreen by remember { mutableStateOf<Rect?>(null) }
        Box(
            Modifier
                .fillMaxSize()
                .onGloballyPositioned { popupOnScreen = it.boundsOnScreen(popupView) }
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f))
                .clickable(
                    interactionSource = null,
                    indication = null,
                    onClickLabel = closeLabel,
                    onClick = onClose
                )
        ) {
            val popup = popupOnScreen ?: return@Box
            val anchor = anchorOnScreen.translate(-popup.topLeft).roundToIntRect()
            MenuFabItems(items, popup, anchor, onClose)
            // The button stays visible above the dimmed page, to close the menu
            Surface(
                onClick = onClose,
                shape = FloatingActionButtonDefaults.extendedFabShape,
                color = containerColor,
                contentColor = contentColor,
                shadowElevation = 6.dp,
                modifier = with(LocalDensity.current) {
                    Modifier
                        .offset { anchor.topLeft }
                        .size(anchor.width.toDp(), anchor.height.toDp())
                        .semantics { contentDescription = closeLabel }
                }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Close, contentDescription = null)
                }
            }
        }
    }
}

/** The items above the button, [popup] and [anchor] are relative to the popup */
@Composable
private fun BoxScope.MenuFabItems(
    items: List<FabMenuItem>,
    popup: Rect,
    anchor: IntRect,
    onClose: () -> Unit,
) {
    val isLtr = LocalLayoutDirection.current == LayoutDirection.Ltr
    with(LocalDensity.current) {
        Column(
            horizontalAlignment = if (isLtr) Alignment.End else Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier
                .align(if (isLtr) Alignment.BottomEnd else Alignment.BottomStart)
                .padding(
                    end = if (isLtr) (popup.width.roundToInt() - anchor.right).toDp() else 0.dp,
                    start = if (isLtr) 0.dp else anchor.left.toDp(),
                    bottom = (popup.height.roundToInt() - anchor.top).toDp() + 16.dp
                )
        ) {
            // The first item is the most used, so it is the lowest, nearest to the button
            for (item in items.asReversed()) {
                FabMenuItemPill(item) {
                    onClose()
                    item.onClick()
                }
            }
        }
    }
}

@Composable
private fun FabMenuItemPill(item: FabMenuItem, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = Shapes.pill,
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
        contentColor = MaterialTheme.colorScheme.onSurface,
        shadowElevation = 3.dp
    ) {
        Row(
            modifier = Modifier
                .height(48.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                item.icon,
                contentDescription = null,
                tint = item.iconTint.takeIf { it != Color.Unspecified }
                    ?: MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(item.label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

/** Lets a popup cover the whole window, its content places itself */
private object WholeWindow : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ) = IntOffset.Zero
}

private fun LayoutCoordinates.boundsOnScreen(view: View): Rect {
    val location = IntArray(2).also(view::getLocationOnScreen)
    return boundsInWindow().translate(Offset(location[0].toFloat(), location[1].toFloat()))
}
