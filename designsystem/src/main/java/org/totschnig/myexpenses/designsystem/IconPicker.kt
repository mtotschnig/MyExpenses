package org.totschnig.myexpenses.designsystem

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Icons shown together under a title in an [IconPicker]
 *
 * @param title null for icons without a heading, e.g. the results of a search
 * @param keys identify the icons, as passed to the icon content of the picker
 */
class IconSection(val title: String?, val keys: List<String>)

/**
 * Picks one of many icons: a search field, filters to show only one theme, and the icons in a
 * grid, the one chosen so far highlighted. The icons themselves come from the caller, so the
 * design system does not depend on a font of icons.
 *
 * @param filters labels of the themes; none selected shows all [sections]
 * @param sections what to show for a search query and the index of the selected filter
 * @param header above the icons, e.g. to go back to the automatic icon
 * @param label of an icon, read by screen readers
 */
@Composable
fun IconPicker(
    filters: List<String>,
    sections: (query: String, filter: Int?) -> List<IconSection>,
    selected: String?,
    onSelect: (String) -> Unit,
    label: (String) -> String?,
    modifier: Modifier = Modifier,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    icon: @Composable (key: String) -> Unit,
) {
    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf<Int?>(null) }
    val shown = remember(query, filter, sections) { sections(query.trim(), filter.takeIf { query.isBlank() }) }
    Column(modifier) {
        SearchField(
            query = query,
            onQueryChange = { query = it },
            placeholder = stringResource(R.string.ds_icon_search),
            clearLabel = stringResource(R.string.ds_icon_search_clear),
            modifier = Modifier.padding(horizontal = Spacing.screen)
        )
        if (query.isBlank() && filters.isNotEmpty()) {
            LazyRow(
                contentPadding = PaddingValues(horizontal = Spacing.screen),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                itemsIndexed(filters) { index, text ->
                    FilterChip(
                        selected = filter == index,
                        onClick = { filter = if (filter == index) null else index },
                        label = { Text(text) }
                    )
                }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(56.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(Spacing.screen),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (header != null && query.isBlank()) {
                item(span = { GridItemSpan(maxLineSpan) }) { Column(content = header) }
            }
            if (shown.all { it.keys.isEmpty() }) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        stringResource(R.string.ds_icon_none_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
            shown.filter { it.keys.isNotEmpty() }.forEach { section ->
                section.title?.let { title ->
                    item(span = { GridItemSpan(maxLineSpan) }, contentType = "title") {
                        GroupTitle(title)
                    }
                }
                items(section.keys, contentType = { "icon" }) { key ->
                    IconChoice(isSelected = key == selected, label = label(key), onClick = { onSelect(key) }) { icon(key) }
                }
            }
        }
    }
}

/** One icon of an [IconPicker] on a tile, the chosen one in the primary container colour with a frame */
@Composable
fun IconChoice(isSelected: Boolean, label: String?, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                selected = isSelected
                label?.let { contentDescription = it }
            }
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        IconTile(
            modifier = if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
            color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
        ) { content() }
    }
}

/**
 * A choice above the icons of an [IconPicker] that is not one of them, e.g. the automatic icon or
 * the logo from a web address: its tile, highlighted like [IconChoice] when chosen, and a text
 */
@Composable
fun IconPickerOption(
    title: String,
    supportingText: String?,
    isSelected: Boolean,
    onClick: () -> Unit,
    tile: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.tile)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { selected = isSelected }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = if (isSelected) Modifier.border(2.dp, MaterialTheme.colorScheme.primary, CircleShape) else Modifier,
        ) { tile() }
        Column(Modifier.padding(start = Spacing.content).weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            supportingText?.let {
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/**
 * An [IconPicker] on a page of its own above everything else, picking an icon closes it
 *
 * @param title what the icon is chosen for, e.g. "Icon for Fitness"
 */
@Composable
fun IconPickerDialog(
    title: String,
    filters: List<String>,
    sections: (query: String, filter: Int?) -> List<IconSection>,
    selected: String?,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
    label: (String) -> String?,
    header: (@Composable ColumnScope.() -> Unit)? = null,
    icon: @Composable (key: String) -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false)
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainer) {
            Column(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                PageTopBar(onBack = onDismiss, title = title)
                IconPicker(
                    filters = filters,
                    sections = sections,
                    selected = selected,
                    onSelect = onSelect,
                    label = label,
                    header = header,
                    modifier = Modifier.weight(1f),
                    icon = icon
                )
            }
        }
    }
}
