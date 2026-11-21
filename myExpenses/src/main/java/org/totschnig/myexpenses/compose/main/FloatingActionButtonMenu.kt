package org.totschnig.myexpenses.compose.main

import androidx.activity.compose.BackHandler
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.FloatingActionButtonMenu
import androidx.compose.material3.FloatingActionButtonMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleFloatingActionButton
import androidx.compose.material3.ToggleFloatingActionButtonDefaults
import androidx.compose.material3.ToggleFloatingActionButtonDefaults.animateIcon
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.compose.calculateOnColor

interface FabMenuAction {
    val imageVector: ImageVector

    @get:StringRes
    val label: Int
    val contentDescription: String
        @Composable get
}

@Composable
fun <T : FabMenuAction> FloatingActionButtonMenu(
    modifier: Modifier = Modifier,
    isStandard: Boolean = true,
    primaryAction: T,
    containerColor: Color = MaterialTheme.colorScheme.surface,
    actions: List<T>,
    onAction: (T) -> Unit
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current

    BackHandler(enabled = expanded) {
        expanded = false
    }

    val overflowDescription = stringResource(
        androidx.appcompat.R.string.abc_action_menu_overflow_description
    )

    FloatingActionButtonMenu(
        expanded = expanded,
        // workaround for https://issuetracker.google.com/issues/440750833
        modifier = modifier.offset(x = 16.dp, y = 16.dp),
        button = {
            if (expanded) {
                ToggleFloatingActionButton(
                    checked = true,
                    onCheckedChange = { expanded = false },
                    containerColor = ToggleFloatingActionButtonDefaults.containerColor(
                        initialColor = containerColor,
                        finalColor = containerColor
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = overflowDescription,
                        modifier = Modifier.animateIcon({ checkedProgress })
                    )
                }
            } else {
                Surface(
                    shape = FloatingActionButtonDefaults.shape,
                    color = containerColor,
                    contentColor = containerColor.calculateOnColor(),
                    shadowElevation = 6.dp
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
                                .combinedClickable(
                                    onClick = { onAction(primaryAction) },
                                    onLongClick = {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expanded = true
                                    }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = primaryAction.imageVector,
                                contentDescription = primaryAction.contentDescription
                            )
                        }

                        if (isStandard) {
                            VerticalDivider(
                                modifier = Modifier
                                    .height(24.dp)
                                    .width(1.dp),
                                color = LocalContentColor.current
                            )

                            Box(
                                modifier = Modifier
                                    .height(56.dp)
                                    .clickable {
                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        expanded = true
                                    }
                                    .padding(start = 8.dp, end = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowDropUp,
                                    contentDescription = overflowDescription
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        actions.reversed().forEach { action ->
            FloatingActionButtonMenuItem(
                onClick = {
                    onAction(action)
                    expanded = false
                },
                text = { Text(stringResource(action.label)) },
                icon = { Icon(action.imageVector, contentDescription = null) },
                containerColor = containerColor,
                contentColor = containerColor.calculateOnColor()
            )
        }
    }
}
