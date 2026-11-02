package org.totschnig.myexpenses.designsystem

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/*
 * Rows of a card. Each row draws a divider above itself, except the first one of a card
 * (isFirst = true).
 */

/**
 * Label on the left, value on the right
 *
 * @param valueColor e.g. for amounts, see [org.totschnig.myexpenses.designsystem.theme.SemanticColors.amountColor]
 */
@Composable
fun InfoRow(label: String, value: String, isFirst: Boolean = false, valueColor: Color = Color.Unspecified) {
    if (!isFirst) RowDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.content, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold,
            color = valueColor,
            textAlign = TextAlign.End,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = Spacing.content)
        )
    }
}

/**
 * Leads to another page, like "show all"
 */
@Composable
fun LinkRow(text: String, onClick: () -> Unit, isFirst: Boolean = false) {
    if (!isFirst) RowDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = Spacing.content, end = 8.dp, top = 16.dp, bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.weight(1f)
        )
        Icon(
            Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary
        )
    }
}

/**
 * A setting that is on or off, with an explanation below the label
 *
 * @param enabled false while another setting turns this one off, e.g. the parts of autofill
 */
@Composable
fun SwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    supportingText: String? = null,
    isFirst: Boolean = false,
    enabled: Boolean = true,
) {
    if (!isFirst) RowDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, enabled = enabled, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(horizontal = Spacing.content, vertical = 12.dp)
            .alpha(if (enabled) 1f else DisabledAlpha),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            supportingText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled,
            modifier = Modifier.padding(start = Spacing.content)
        )
    }
}

/**
 * A setting whose value is picked or typed in a dialog: the label with the current value below.
 * Unlike [EditableRow] the value may be long, e.g. a list of date formats.
 *
 * @param value null for an action without value, e.g. deleting a calendar
 * @param enabled false while another setting turns this one off
 */
@Composable
fun ValueRow(
    label: String,
    value: String?,
    onClick: () -> Unit,
    isFirst: Boolean = false,
    enabled: Boolean = true,
) {
    if (!isFirst) RowDivider()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClickLabel = label, onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.content, vertical = 12.dp)
            .alpha(if (enabled) 1f else DisabledAlpha),
        verticalArrangement = Arrangement.Center
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        value?.takeIf { it.isNotEmpty() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * A setting with a value in a range, e.g. the quality of pictures. The value is handed over when
 * the user lets go, not on every step of dragging.
 *
 * @param valueText the value as shown next to the label, e.g. "80 %"
 * @param steps between the ends of [valueRange], 0 for a continuous value
 * @param preview below the slider, of the value while dragging, e.g. a text in a font size
 */
@Composable
fun SliderRow(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    valueText: (Float) -> String,
    isFirst: Boolean = false,
    steps: Int = 0,
    enabled: Boolean = true,
    preview: (@Composable (Float) -> Unit)? = null,
) {
    if (!isFirst) RowDivider()
    var current by remember(value) { mutableFloatStateOf(value) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = Spacing.content, end = Spacing.content, top = 12.dp)
            .alpha(if (enabled) 1f else DisabledAlpha)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            Text(
                valueText(current),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = Spacing.content)
            )
        }
        Slider(
            value = current,
            onValueChange = { current = it },
            onValueChangeFinished = { onValueChange(current) },
            valueRange = valueRange,
            steps = steps,
            enabled = enabled,
            modifier = Modifier.semantics { contentDescription = label }
        )
        preview?.let { Box(Modifier.padding(bottom = 12.dp)) { it(current) } }
    }
}

/**
 * Explains something within a card, e.g. that backups to the cloud are not encrypted
 */
@Composable
fun HintRow(text: String, isFirst: Boolean = false, icon: ImageVector = Icons.Outlined.Info) {
    if (!isFirst) RowDivider()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.content, vertical = 12.dp)
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = Spacing.content)
        )
    }
}

/**
 * Entry of a page that lists other pages, e.g. the groups of the settings: icon, title and what
 * can be found there. Unlike the rows above it is not part of a card and has no divider.
 *
 * @param supportingText e.g. a few of the settings in the group, null for a single line
 */
@Composable
fun IconNavigationRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
) {
    IconNavigationRow(title, onClick, modifier, supportingText) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/**
 * [IconNavigationRow] with an icon that is no vector, e.g. of a font
 *
 * @param icon 24dp
 */
@Composable
fun IconNavigationRow(
    title: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    supportingText: String? = null,
    icon: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 56.dp)
            .padding(horizontal = Spacing.content, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(Modifier.size(24.dp), contentAlignment = Alignment.Center) { icon() }
        Column(
            Modifier
                .weight(1f)
                .padding(start = 32.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            supportingText?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

/** Of rows that another setting turns off */
private const val DisabledAlpha = 0.38f

