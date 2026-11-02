package org.totschnig.myexpenses.next.core.ui.icons

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import org.totschnig.myexpenses.designsystem.IconPickerDialog
import org.totschnig.myexpenses.designsystem.IconPickerOption
import org.totschnig.myexpenses.designsystem.IconSection
import org.totschnig.myexpenses.designsystem.IconTile
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.LocalFontIcons
import org.totschnig.myexpenses.next.core.common.icons.CatalogIcon
import org.totschnig.myexpenses.next.core.common.icons.IconCatalog

/**
 * An icon of the [IconCatalog] or any other of Font Awesome, in the colour of icons on tiles
 *
 * @param isLarge in the header of details, small in lists
 */
@Composable
fun CatalogIcon(name: String, modifier: Modifier = Modifier, isLarge: Boolean = false) {
    LocalFontIcons.current.Icon(name, modifier, if (isLarge) 28.sp else 20.sp, MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A [CatalogIcon] on an [IconTile] */
@Composable
fun CatalogIconTile(name: String, modifier: Modifier = Modifier, isLarge: Boolean = false) {
    IconTile(modifier, isLarge) { CatalogIcon(name, isLarge = isLarge) }
}

/**
 * Lets the user pick an icon for something, e.g. a pot: [suggestions] first, then the themes of
 * the [IconCatalog]; a search also finds all other icons of Font Awesome by their English label.
 *
 * @param selected the icon the user chose before, null if the icon is chosen automatically
 * @param onSelect null to choose the icon automatically again
 * @param isAutomatic whether the automatic choice is highlighted, e.g. not for a payee with a web address
 * @param automatic shows the icon chosen automatically, null if there is none, e.g. for a new pot
 * @param automaticHint how the automatic icon is chosen
 * @param header further choices, e.g. the logo of a web address
 */
@Composable
fun CatalogIconPicker(
    title: String,
    selected: String?,
    suggestions: List<String>,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
    isAutomatic: Boolean = selected == null,
    automatic: (@Composable () -> Unit)? = null,
    automaticHint: String = stringResource(R.string.next_icons_automatic_hint),
    header: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val labels = LocalFontIcons.current.labels()
    val themes = IconCatalog.themes.map { stringResource(it.labelRes) }
    val suggestedTitle = stringResource(R.string.next_icons_suggested)
    val distinctSuggestions = remember(suggestions) { suggestions.distinct() }
    val filters = (if (distinctSuggestions.isEmpty()) emptyList() else listOf(suggestedTitle)) + themes
    val sections = remember(distinctSuggestions, themes, labels) {
        { query: String, filter: Int? ->
            val all = buildList {
                if (distinctSuggestions.isNotEmpty()) add(IconSection(suggestedTitle, distinctSuggestions))
                IconCatalog.themes.forEachIndexed { index, theme ->
                    add(IconSection(themes[index], theme.icons.map { it.name }))
                }
            }
            when {
                query.isNotEmpty() -> listOf(IconSection(null, IconCatalog.search(query, labels)))
                filter != null -> listOfNotNull(all.getOrNull(filter))
                else -> all
            }
        }
    }
    IconPickerDialog(
        title = title,
        filters = filters,
        sections = sections,
        selected = selected,
        onSelect = onSelect,
        onDismiss = onDismiss,
        label = labels::get,
        header = if (automatic != null || header != null) {
            {
                if (automatic != null) {
                    IconPickerOption(
                        title = stringResource(R.string.next_icons_automatic),
                        supportingText = automaticHint,
                        isSelected = isAutomatic,
                        onClick = { onSelect(null) },
                        tile = automatic
                    )
                }
                header?.invoke(this)
            }
        } else null
    ) { name -> CatalogIcon(name) }
}
