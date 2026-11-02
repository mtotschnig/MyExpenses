package org.totschnig.myexpenses.designsystem

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/** From this width on sheets come in from the side: tablets and phones in landscape */
private val WideWindowWidth = 600.dp

/** Width of a side sheet, as far as the window allows */
private val SideSheetWidth = 400.dp

/** Kept free beside a side sheet, so that the screen below can still be seen */
private val SideSheetMinGap = 56.dp

/** Lists with more options get a search field */
private const val SEARCH_THRESHOLD = 8

/**
 * Whether the window is wide enough for sheets from the side and content side by side, e.g. a
 * tablet or a phone in landscape
 */
@Composable
fun isWideWindow(): Boolean = with(LocalDensity.current) {
    LocalWindowInfo.current.containerSize.width.toDp() >= WideWindowWidth
}

/**
 * Content that belongs to the screen below and is shown on top of it for a moment, e.g. a filter
 * or a choice. Comes up from the bottom in narrow windows and in from the end in wide ones
 * ([isWideWindow]), where a sheet from the bottom would be too low or too wide.
 *
 * @param title above the content
 * @param actions at the bottom, stay visible while the content scrolls, e.g. apply and reset
 * @param scrollable false if [content] scrolls by itself, e.g. a lazy list
 * @param skipPartiallyExpanded false lets a sheet from the bottom stop at half the height first
 * @param closeLabel shown as a close button next to the title, e.g. when the actions have no way
 * to dismiss; a side sheet always has one
 * @param content scrolls if it does not fit
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdaptiveSheet(
    onDismiss: () -> Unit,
    title: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    scrollable: Boolean = true,
    skipPartiallyExpanded: Boolean = true,
    closeLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (isWideWindow()) {
        SideSheet(onDismiss = onDismiss, title = title, closeLabel = closeLabel) {
            SheetBody(actions, scrollable, content)
        }
    } else {
        ModalBottomSheet(
            onDismissRequest = onDismiss,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = skipPartiallyExpanded),
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            if (closeLabel != null) {
                TitleRow(title, closeLabel, onDismiss)
            } else title?.let { SheetTitle(it, Modifier.padding(horizontal = Spacing.screen, vertical = 8.dp)) }
            SheetBody(actions, scrollable, content)
        }
    }
}

/**
 * A modal sheet at the end of the window, over a scrim: tapping beside it or going back closes it.
 * Compose Material 3 has none, so it is a dialog filling the window.
 */
@Composable
private fun SideSheet(
    onDismiss: () -> Unit,
    title: String?,
    closeLabel: String?,
    content: @Composable ColumnScope.() -> Unit,
) {
    val visible = remember { MutableTransitionState(false).apply { targetState = true } }
    val close = { visible.targetState = false }
    // Closes after sliding out
    LaunchedEffect(visible.isIdle, visible.currentState) {
        if (visible.isIdle && !visible.currentState) onDismiss()
    }
    Dialog(
        onDismissRequest = close,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        // The scrim below is drawn by the sheet so that it fades with it
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect { window?.setDimAmount(0f) }
        Box(Modifier.fillMaxSize()) {
            AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = close)
                )
            }
            val towardsEnd = if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1
            AnimatedVisibility(
                visible,
                modifier = Modifier.align(Alignment.CenterEnd),
                enter = slideInHorizontally { it * towardsEnd },
                exit = slideOutHorizontally { it * towardsEnd }
            ) {
                SideSheetPanel(title = title, onClose = close, closeLabel = closeLabel, content = content)
            }
        }
    }
}

/**
 * The panel of a side sheet, without window and animation, to be shown in previews
 */
@Composable
internal fun SideSheetPanel(
    title: String?,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    closeLabel: String? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier) {
        Surface(
            modifier = Modifier
                .width(minOf(SideSheetWidth, maxWidth - SideSheetMinGap))
                .fillMaxHeight(),
            shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                TitleRow(title, closeLabel ?: stringResource(R.string.ds_close), onClose)
                content()
            }
        }
    }
}

/** [title] with a close button at the end */
@Composable
private fun TitleRow(title: String?, closeLabel: String, onClose: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(start = Spacing.screen, end = 4.dp, top = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SheetTitle(title.orEmpty(), Modifier.weight(1f))
        IconButton(onClick = onClose) {
            Icon(Icons.Default.Close, contentDescription = closeLabel)
        }
    }
}

@Composable
private fun SheetTitle(title: String, modifier: Modifier = Modifier) {
    Text(title, style = MaterialTheme.typography.titleLarge, modifier = modifier)
}

/**
 * Content and actions of a sheet: the content scrolls, the actions stay below it
 */
@Composable
internal fun ColumnScope.SheetBody(
    actions: (@Composable RowScope.() -> Unit)?,
    scrollable: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        Modifier
            .weight(1f, fill = false)
            .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
    ) {
        content()
    }
    if (actions != null) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.screen, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
            content = actions
        )
    }
}

/**
 * Lets the user pick any number of [options]; the choice is applied on confirming. Long lists can
 * be searched by their labels.
 *
 * @param supportingText second line of an option, e.g. what it means
 * @param intro shown above the options
 * @param header above the options, e.g. a name for what is chosen
 * @param footer below the options, e.g. to add a new option
 */
@Composable
fun <T> MultiChoiceSheet(
    title: String,
    options: List<T>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    label: @Composable (T) -> String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    supportingText: @Composable (T) -> String? = { null },
    intro: String? = null,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
    confirmEnabled: Boolean = true,
) {
    AdaptiveSheet(
        onDismiss = onDismiss,
        title = title,
        actions = { ConfirmActions(onConfirm, confirmEnabled) { TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) } } }
    ) {
        MultiChoiceContent(options, selected, onToggle, label, supportingText, intro, header, footer)
    }
}

/**
 * Two actions at the bottom of a sheet, Apply and before it [dismiss], e.g. Cancel or Skip
 */
@Composable
private fun ConfirmActions(
    onConfirm: () -> Unit,
    confirmEnabled: Boolean,
    confirmLabel: String? = null,
    dismiss: @Composable () -> Unit,
) {
    dismiss()
    Button(onClick = onConfirm, enabled = confirmEnabled) { Text(confirmLabel ?: stringResource(R.string.ds_apply)) }
}

@Composable
internal fun <T> MultiChoiceContent(
    options: List<T>,
    selected: Set<T>,
    onToggle: (T) -> Unit,
    label: @Composable (T) -> String,
    supportingText: @Composable (T) -> String?,
    intro: String? = null,
    header: (@Composable () -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val labels = options.map { it to label(it) }
    val shown = if (query.isBlank()) labels else labels.filter { it.second.contains(query.trim(), ignoreCase = true) }
    if (intro != null || header != null) {
        Column(Modifier.padding(horizontal = Spacing.screen)) {
            intro?.let { DialogIntro(it) }
            header?.invoke()
        }
    }
    if (options.size > SEARCH_THRESHOLD) {
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.ds_search),
            clearLabel = stringResource(R.string.ds_icon_search_clear),
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 8.dp)
        )
    }
    shown.forEach { (option, text) ->
        OptionRow(
            label = text,
            selected = option in selected,
            onClick = { onToggle(option) },
            supportingText = supportingText(option)
        )
    }
    if (shown.isEmpty()) {
        Text(
            stringResource(R.string.ds_none_found),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 12.dp)
        )
    }
    footer?.let {
        Column(Modifier.padding(horizontal = Spacing.screen)) { it() }
    }
}

/**
 * An action in an [ActionSheet]
 *
 * @param subActions shown instead of the actions when picked, with a way back
 * @param onClick called after the sheet is closed, not for an action with [subActions]
 */
@Immutable
class SheetAction(
    val label: String,
    val icon: (@Composable () -> Painter)? = null,
    val tint: Color? = null,
    val testTag: String? = null,
    val contentDescription: String? = null,
    val subActions: List<SheetAction> = emptyList(),
    /** Whether an action that switches something on and off is on, announced as a checkbox */
    val checked: Boolean? = null,
    val onClick: () -> Unit = {},
)

/**
 * The actions of something, e.g. of a transaction, from the bottom. Actions with sub actions
 * open them as a second level.
 *
 * @param title above the actions, e.g. what they apply to
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActionSheet(
    actions: List<SheetAction>,
    onDismiss: () -> Unit,
    title: String? = null,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        contentWindowInsets = { WindowInsets.navigationBars }
    ) {
        ActionSheetContent(actions, title, onDismiss)
    }
}

@Composable
internal fun ActionSheetContent(actions: List<SheetAction>, title: String?, onDismiss: () -> Unit) {
    var opened by remember { mutableStateOf<SheetAction?>(null) }
    Column(
        Modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp)
    ) {
        val parent = opened
        if (parent != null) {
            ActionRow(parent.label, icon = rememberBackIcon(), onClick = { opened = null }, isTitle = true)
            parent.subActions.forEach { action -> ActionSheetRow(action, onDismiss) { opened = it } }
        } else {
            title?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 8.dp)
                )
            }
            actions.forEach { action -> ActionSheetRow(action, onDismiss) { opened = it } }
        }
    }
}

@Composable
private fun rememberBackIcon(): @Composable () -> Painter = {
    rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack)
}

@Composable
private fun ActionSheetRow(action: SheetAction, onDismiss: () -> Unit, onOpen: (SheetAction) -> Unit) {
    val hasSub = action.subActions.isNotEmpty()
    ActionRow(
        label = action.label,
        icon = action.icon,
        tint = action.tint,
        modifier = Modifier
            .then(action.testTag?.let { Modifier.testTag(it) } ?: Modifier)
            .then(action.contentDescription?.let { Modifier.semantics { contentDescription = it } } ?: Modifier),
        hasSub = hasSub,
        checked = action.checked,
        onClick = {
            if (hasSub) onOpen(action) else {
                onDismiss()
                action.onClick()
            }
        }
    )
}

@Composable
private fun ActionRow(
    label: String,
    icon: (@Composable () -> Painter)?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
    hasSub: Boolean = false,
    isTitle: Boolean = false,
    checked: Boolean? = null,
) {
    Row(
        modifier
            .fillMaxWidth()
            .then(
                if (checked != null) Modifier.toggleable(checked, role = Role.Checkbox, onValueChange = { onClick() })
                else Modifier.clickable(onClick = onClick)
            )
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.screen),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(icon(), contentDescription = null, tint = tint ?: MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(Spacing.content))
        } else {
            Spacer(Modifier.width(24.dp + Spacing.content))
        }
        Text(
            label,
            style = if (isTitle) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyLarge,
            color = LocalContentColor.current,
            modifier = Modifier.weight(1f)
        )
        if (hasSub) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

