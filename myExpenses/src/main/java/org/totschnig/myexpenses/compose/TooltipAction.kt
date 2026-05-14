package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.dialog.MenuItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    tooltip: String,
    imageVector: ImageVector,
    isChecked: Boolean = false,
    onClick: () -> Unit,
) {
    TooltipIconButton(
        tooltip = tooltip,
        painter = rememberVectorPainter(imageVector),
        isChecked = isChecked,
        onClick = onClick
    )
}

@Composable
fun TooltipIconButton(
    menuItem: MenuItem,
    isChecked: Boolean = false,
    onClick: () -> Unit,
) {
    TooltipIconButton(
        tooltip = menuItem.getLabel(LocalContext.current),
        painter = menuItem.painter,
        modifier = Modifier.testTag(menuItem.testTag),
        isChecked = isChecked,
        onClick = onClick
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    tooltip: String,
    painter: Painter,
    modifier: Modifier = Modifier,
    isChecked: Boolean = false,
    onClick: () -> Unit,
) {
    TooltipBox(
        modifier = modifier,
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(
            TooltipAnchorPosition.Below,
            4.dp
        ),
        tooltip = {
            PlainTooltip {
                Text(tooltip)
            }
        },
        state = rememberTooltipState()
    ) {
        if (isChecked) {
            FilledTonalIconButton(onClick = onClick) {
                Icon(painter, tooltip)
            }
        } else {
            IconButton(onClick = onClick) {
                Icon(painter, tooltip)
            }
        }
    }
}

@Composable
fun TooltipIconMenu(
    modifier: Modifier = Modifier,
    tooltip: String,
    imageVector: ImageVector,
    menu: Menu,
) {
    Box(modifier) {
        val showMenu = remember { mutableStateOf(false) }

        TooltipIconButton(
            tooltip = tooltip,
            imageVector = imageVector
        ) { showMenu.value = true }

        HierarchicalMenu(
            expanded = showMenu,
            menu = menu,
            title = tooltip
        )
    }
}
