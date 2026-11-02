package org.totschnig.myexpenses.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

/**
 * Bar at the top of a page: back, optional title and subtitle, actions on the right
 *
 * @param onBack null for a page without back, e.g. the content of a tab of the bottom bar
 */
@Composable
fun PageTopBar(
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        // As high with or without back
        modifier = modifier
            .padding(4.dp)
            .heightIn(min = 48.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onBack != null) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.ds_back))
            }
        }
        Column(
            Modifier
                .weight(1f)
                // Without back, the title lines up with the cards
                .padding(start = if (onBack != null) 4.dp else Spacing.screen - 4.dp, end = Spacing.content)
        ) {
            title?.let {
                Text(
                    it,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            subtitle?.invoke()
        }
        actions()
    }
}

/** An entry of a [MenuButton] */
class MenuEntry(val label: String, val icon: ImageVector, val onClick: () -> Unit)

/**
 * Three dots that open a menu, e.g. for the rare actions on a group of items like sorting them
 */
@Composable
fun MenuButton(entries: List<MenuEntry>, contentDescription: String) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }) {
            Icon(Icons.Default.MoreVert, contentDescription = contentDescription, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            entries.forEach { entry ->
                DropdownMenuItem(
                    text = { Text(entry.label) },
                    leadingIcon = { Icon(entry.icon, contentDescription = null) },
                    onClick = {
                        expanded = false
                        entry.onClick()
                    }
                )
            }
        }
    }
}

/**
 * A page of its own showing details: [PageTopBar] and scrolling content of cards
 *
 * @param header stays in place below the top bar while the content scrolls, e.g. a sum
 * @param scrollable false if the content scrolls by itself, e.g. two columns side by side
 */
@Composable
fun DetailScaffold(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: (@Composable () -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    header: (@Composable ColumnScope.() -> Unit)? = null,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainer)
    ) {
        PageTopBar(onBack = onBack, title = title, subtitle = subtitle, actions = actions)
        if (header != null) {
            Column(Modifier.padding(horizontal = Spacing.screen), content = header)
        }
        Column(
            Modifier
                .weight(1f)
                .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                .padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.screen),
            content = content
        )
    }
}
