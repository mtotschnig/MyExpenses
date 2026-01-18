package org.totschnig.myexpenses.compose

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TooltipIconButton(
    tooltip: String,
    imageVector: ImageVector,
    onClick: () -> Unit
) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Below, 4.dp),
        tooltip = {
            PlainTooltip {
                Text(tooltip)
            }
        },
        state = rememberTooltipState()
    ) {
        IconButton(onClick = onClick) {
            Icon(imageVector, tooltip)
        }
    }
}

@Composable
fun TooltipIconMenu(
    modifier: Modifier = Modifier,
    tooltip: String,
    imageVector: ImageVector,
    menu: Menu
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
