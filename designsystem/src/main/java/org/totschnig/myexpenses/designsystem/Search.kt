package org.totschnig.myexpenses.designsystem

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Top bar while a page is searched: a field for the text in place of the title, the back arrow
 * ends the search. The field gets the focus when the bar appears.
 *
 * @param closeLabel describes the back arrow
 * @param clearLabel describes the button that empties the field
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    placeholder: String,
    onClose: () -> Unit,
    closeLabel: String,
    clearLabel: String,
    windowInsets: WindowInsets = TopAppBarDefaults.windowInsets,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
) {
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    TopAppBar(
        windowInsets = windowInsets,
        colors = TopAppBarDefaults.topAppBarColors(containerColor = containerColor),
        navigationIcon = {
            IconButton(onClick = onClose) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = closeLabel)
            }
        },
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = 8.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text(placeholder, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                trailingIcon = if (query.isEmpty()) null else {
                    {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = clearLabel)
                        }
                    }
                },
                singleLine = true,
                shape = Shapes.pill,
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                )
            )
        }
    )
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

/**
 * The [SearchFilterChip]s of a search in one line that scrolls sideways
 */
@Composable
fun SearchFilterRow(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(horizontal = Spacing.screen),
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(contentPadding),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

/**
 * One kind of filter of a search, e.g. the period. Unset it shows the kind, set it shows the
 * value and is filled. Tapping it opens the choice of the value, usually a [FilterSheet].
 *
 * @param value what is chosen, e.g. "October 2026", null if the filter is not set
 */
@Composable
fun SearchFilterChip(
    label: String,
    value: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    FilterChip(
        selected = value != null,
        onClick = onClick,
        label = { Text(value ?: label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingIcon = icon?.let {
            { Icon(it, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize)) }
        },
        trailingIcon = {
            Icon(Icons.Default.ArrowDropDown, contentDescription = null, modifier = Modifier.size(FilterChipDefaults.IconSize))
        },
        modifier = modifier
    )
}

/**
 * [AdaptiveSheet] that chooses the value of one filter, e.g. the categories to search.
 * Below the content the filter is reset or applied.
 *
 * @param onReset removes the filter, null while it is not set
 * @param applyEnabled false while the choice is incomplete, e.g. no category is checked
 */
@Composable
fun FilterSheet(
    title: String,
    onDismiss: () -> Unit,
    onApply: () -> Unit,
    applyLabel: String,
    resetLabel: String,
    onReset: (() -> Unit)?,
    applyEnabled: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    AdaptiveSheet(
        onDismiss = onDismiss,
        title = title,
        actions = {
            if (onReset != null) {
                TextButton(onClick = onReset) { Text(resetLabel) }
            }
            Button(onClick = onApply, enabled = applyEnabled) { Text(applyLabel) }
        },
        content = content
    )
}

/**
 * An option in a [FilterSheet] that is checked or picked, e.g. a category
 *
 * @param isMultiple whether several options can be checked: a checkbox, else a radio button
 * @param leading e.g. the icon of a category
 * @param depth indents options below others, e.g. subcategories
 */
@Composable
fun OptionRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    isMultiple: Boolean = true,
    supportingText: String? = null,
    depth: Int = 0,
    leading: (@Composable () -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (isMultiple) Modifier.toggleable(value = selected, role = Role.Checkbox, onValueChange = { onClick() })
                else Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            )
            .heightIn(min = 48.dp)
            .padding(start = Spacing.screen + 24.dp * depth, end = Spacing.screen, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (leading != null) {
            leading()
            Spacer(Modifier.size(12.dp))
        }
        Column(Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (isMultiple) Checkbox(checked = selected, onCheckedChange = null)
        else RadioButton(selected = selected, onClick = null)
    }
}

/**
 * What a search found, e.g. "23 transactions · −412,30 €", with an action to reset it
 */
@Composable
fun SearchResultRow(
    text: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Spacing.screen, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(onClick = onAction) { Text(actionLabel) }
    }
}
