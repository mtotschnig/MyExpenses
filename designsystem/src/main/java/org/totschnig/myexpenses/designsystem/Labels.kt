package org.totschnig.myexpenses.designsystem

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.ui.semantics.Role
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.FlowRowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.layout
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * How a [TextBadge] looks
 */
enum class BadgeStyle {
    /** Something that is set, e.g. the category of a transaction */
    Filled,

    /** Something only suggested, not set yet: lightly outlined, in italics and with a question mark */
    Suggestion,

    /** Something set that adds context, e.g. a tag next to the category: outlined */
    Outlined,

    /** Something that should be set but is not yet, e.g. an item without group: outlined in error colour */
    Missing
}

/**
 * Small box with a short text, e.g. the category in the second line of a list item
 *
 * @param icon tells what kind of thing the text is, e.g. a group
 */
@Composable
fun TextBadge(
    text: String,
    modifier: Modifier = Modifier,
    style: BadgeStyle = BadgeStyle.Filled,
    icon: ImageVector? = null,
) {
    Surface(
        shape = Shapes.badge,
        color = if (style == BadgeStyle.Filled) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
        contentColor = when (style) {
            BadgeStyle.Filled -> MaterialTheme.colorScheme.onSecondaryContainer
            BadgeStyle.Suggestion -> MaterialTheme.colorScheme.onSurfaceVariant
            BadgeStyle.Outlined -> MaterialTheme.colorScheme.onSurfaceVariant
            BadgeStyle.Missing -> MaterialTheme.colorScheme.error
        },
        border = when (style) {
            BadgeStyle.Filled -> null
            BadgeStyle.Suggestion -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            BadgeStyle.Outlined -> BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            BadgeStyle.Missing -> BorderStroke(1.dp, MaterialTheme.colorScheme.error)
        },
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 1.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    modifier = Modifier
                        .size(16.dp)
                        .padding(end = 4.dp)
                )
            }
            Text(
                if (style == BadgeStyle.Suggestion) "$text?" else text,
                style = MaterialTheme.typography.labelMedium,
                fontStyle = if (style == BadgeStyle.Suggestion) FontStyle.Italic else null,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A property that is on or off, e.g. whether an expense was one-off: filled with a check mark
 * when on, outlined when off. Several of them sit next to each other.
 */
@Composable
fun ToggleChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(text) },
        modifier = modifier,
        leadingIcon = if (selected) {
            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        } else null,
        shape = Shapes.chip
    )
}

/**
 * Picks one of [options] from chips that wrap into several lines, e.g. the group of an item.
 * Fits where a list of radio buttons would be too long.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun <T> ChoiceChips(
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.forEach { option ->
            ToggleChip(text = label(option), selected = option == selected, onClick = { onSelect(option) })
        }
    }
}

/**
 * Chips of one kind with a short label in front, e.g. "Wofür" before the category. Several of
 * them below each other line up their chips.
 *
 * @param hint explains the chips. Only needed now and then, so it stays hidden until the label,
 * which then has an info icon, is tapped.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ChipRow(
    label: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
    content: @Composable FlowRowScope.() -> Unit,
) {
    var showHint by rememberSaveable { mutableStateOf(false) }
    Row(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .width(ChipRowLabelWidth)
                .padding(end = 8.dp)
                .then(
                    if (hint != null) Modifier
                        .clip(Shapes.badge)
                        .toggleable(value = showHint, role = Role.Button, onValueChange = { showHint = it })
                    else Modifier
                )
                // As high as the touch area of a chip, so the label stays next to the first row of chips
                .heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f, fill = false)
            )
            if (hint != null) {
                Icon(
                    if (showHint) Icons.Filled.Info else Icons.Outlined.Info,
                    contentDescription = stringResource(R.string.ds_show_hint),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .size(16.dp)
                )
            }
        }
        Column(Modifier.weight(1f)) {
            // Chips take 48 dp to be easy to tap, which already leaves a gap between their lines
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                content = content
            )
            if (hint != null) {
                AnimatedVisibility(showHint) {
                    Text(
                        hint,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * The rarely needed actions at the end of the details of something, as [ActionChip]s that wrap
 * into several lines, set apart like a section. Takes no room while there is no chip in it, so
 * the chips can be left out one by one when they do not apply.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ActionChipRow(
    modifier: Modifier = Modifier,
    content: @Composable FlowRowScope.() -> Unit,
) {
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .layout { measurable, constraints ->
                val top = Spacing.section.roundToPx()
                val placeable = measurable.measure(constraints)
                if (placeable.height == 0) layout(placeable.width, 0) {}
                else layout(placeable.width, placeable.height + top) { placeable.place(0, top) }
            },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content
    )
}

private val ChipRowLabelWidth = 88.dp

/**
 * Marks something new, e.g. unread news
 */
@Composable
fun Dot(modifier: Modifier = Modifier, color: Color = MaterialTheme.colorScheme.error) {
    Box(
        modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color)
    )
}

