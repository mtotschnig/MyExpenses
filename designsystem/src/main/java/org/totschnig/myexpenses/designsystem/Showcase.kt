package org.totschnig.myexpenses.designsystem

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Workspaces
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.designsystem.theme.DesignSystemTheme
import org.totschnig.myexpenses.designsystem.theme.LocalSemanticColors

/**
 * All building blocks on one page, to check how they look together. The charts have their own
 * previews in `:charts`.
 */
@Preview(name = "Light", showBackground = true, heightDp = 2600)
@Preview(name = "Dark", showBackground = true, heightDp = 2600, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun Showcase() {
    DesignSystemTheme {
        DetailScaffold(onBack = {}, title = "Design system") {
            PageTopBar(onBack = null, title = "Page without back")
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextBadge("Badge")
                TextBadge("Suggestion", Modifier.padding(start = 8.dp), style = BadgeStyle.Suggestion)
                TextBadge("Group", Modifier.padding(start = 8.dp), style = BadgeStyle.Outlined)
                TextBadge("Missing", Modifier.padding(start = 8.dp), style = BadgeStyle.Missing)
                Dot(Modifier.padding(start = 8.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                ToggleChip(text = "On", selected = true, onClick = {})
                ToggleChip(text = "Off", selected = false, onClick = {}, modifier = Modifier.padding(start = 8.dp))
                TextBadge("With icon", Modifier.padding(start = 8.dp), style = BadgeStyle.Outlined, icon = Icons.Outlined.Workspaces)
            }
            DetailCard {
                Column(Modifier.padding(Spacing.content), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ChipRow("With hint", hint = "Explains the chips, shown after tapping the label") {
                        ToggleChip(text = "On", selected = true, onClick = {})
                        ToggleChip(text = "Off", selected = false, onClick = {})
                    }
                    ChoiceChips(
                        options = listOf("Choice", "Chips", "Pick one", "Wrap into lines"),
                        selected = "Chips",
                        label = { it },
                        onSelect = {}
                    )
                }
            }
            MenuFab(
                label = "Create", icon = Icons.Outlined.Folder, menuLabel = "Choose", closeLabel = "Close",
                items = listOf(FabMenuItem("Kind", Icons.Outlined.Info) {}),
                modifier = Modifier.align(Alignment.End)
            )
            InviteCard(
                icon = Icons.Outlined.Info, title = "Invite", text = "Something optional to start, can be closed",
                onClick = {}, onDismiss = {}, dismissLabel = "Close"
            )
            GroupTitle("Group with menu", action = {
                MenuButton(listOf(MenuEntry("Entry", Icons.Outlined.Folder) {}), contentDescription = "More")
            })
            SectionTitle("Rows")
            DetailCard {
                InfoRow("Info", "Value", isFirst = true)
                SwitchRow("Switch", checked = true, onCheckedChange = {}, supportingText = "Explanation")
                LinkRow("Link", onClick = {})
                LoadingRow()
            }
            SectionTitle("Settings")
            SearchField(query = "", onQueryChange = {}, placeholder = "Search", clearLabel = "Clear")
            IconNavigationRow(Icons.Outlined.Palette, "Icon navigation", onClick = {}, supportingText = "Lists what can be found there")
            IconNavigationRow(Icons.Outlined.Info, "Single line", onClick = {})
            DetailCard {
                ValueRow("Value", "Current value", onClick = {}, isFirst = true)
                ValueRow("Action without value", null, onClick = {})
                SwitchRow("Turns others off", checked = false, onCheckedChange = {})
                SwitchRow("Disabled switch", checked = true, onCheckedChange = {}, enabled = false)
                ValueRow("Disabled value", "Current value", onClick = {}, enabled = false)
                SliderRow("Slider", 80f, onValueChange = {}, valueRange = 0f..100f, valueText = { "${it.toInt()} %" })
                SliderRow(
                    "Slider with steps and preview", 1f, onValueChange = {}, valueRange = 0f..3f, steps = 2,
                    valueText = { "+${it.toInt() * 10} %" },
                    preview = { Text("Sample text", fontSize = 16.sp * (1 + it / 10)) }
                )
                HintRow("Hint explaining something within a card")
                var isHighlighted by remember { mutableStateOf(true) }
                Highlighted(isHighlighted, onShown = { isHighlighted = false }) {
                    ValueRow("Highlighted", "Tap to light up again", onClick = { isHighlighted = true })
                }
            }
            SectionTitle("Group")
            for (index in 0 until 3) {
                GroupItem(GroupPosition.of(index, 3)) {
                    Text("Item ${index + 1}", Modifier.padding(Spacing.content))
                }
            }
            SectionTitle("Semantic colors")
            DetailCard {
                val colors = LocalSemanticColors.current
                InfoRow("Income", "+12,00 €", isFirst = true, valueColor = colors.income)
                InfoRow("Expense", "−12,00 €", valueColor = colors.expense)
                InfoRow("Transfer", "12,00 €", valueColor = colors.transfer)
            }
            SectionTitle("Overview")
            OverviewCard("Overview card", onClick = {}) {
                Text("Content of a card of the overview")
            }
            OverviewCard("With action", actionLabel = "Show all") {
                Text("A link next to the title")
            }
            OverviewCard("Loading") {
                LoadingBlock(Modifier.fillMaxWidth(0.7f))
                LoadingBlock(Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            GroupTitle("Group title", action = {
                IconButton(onClick = {}) { Icon(Icons.Default.Add, contentDescription = null) }
            })
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconTile { Icon(Icons.Default.Search, contentDescription = null) }
                IconTile(Modifier.padding(start = 8.dp), isLarge = true) { Icon(Icons.Default.Search, contentDescription = null) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                listOf("REWE", "Stadtwerke", "Netflix", "Bäckerei").forEach {
                    PayeeTile(it, logo = null, Modifier.padding(end = 8.dp)) {}
                }
                PayeeTile(null, logo = null, Modifier.padding(end = 8.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null)
                }
                PayeeTile("REWE", logo = null, isLarge = true) {}
            }
            ActionCard(
                icon = Icons.Default.Search,
                title = "Action card",
                text = "Asks to start something within a page",
                confirmLabel = "Add",
                onConfirm = {},
                confirmIcon = Icons.Default.Add
            )
            EmptyState("Nothing here", hint = "Shown instead of an empty list", modifier = Modifier.size(320.dp, 160.dp))
        }
    }
}

@Preview(name = "Text dialog", showBackground = true)
@Composable
private fun TextDialogShowcase() {
    DesignSystemTheme {
        TextDialog(
            title = "Text dialog",
            initialText = "dd.MM.yyyy",
            onConfirm = {},
            onDismiss = {},
            supportingText = "What the text is used for"
        )
    }
}

@Preview(name = "Text dialog with preview", showBackground = true)
@Composable
private fun TextDialogPreviewShowcase() {
    DesignSystemTheme {
        TextDialog(
            title = "Own format",
            initialText = "dd.MM.yyyy",
            onConfirm = {},
            onDismiss = {},
            intro = "d = day, M = month, y = year",
            preview = { "Example: 11.10.2026" },
            error = { if ('d' in it) null else "The day is missing" }
        )
    }
}

@Preview(name = "Time dialog", showBackground = true, heightDp = 640)
@Composable
private fun TimeDialogShowcase() {
    DesignSystemTheme {
        TimeDialog(title = "Time dialog", hour = 3, minute = 30, onConfirm = { _, _ -> }, onDismiss = {})
    }
}

@Preview(name = "Date dialog", showBackground = true)
@Composable
private fun DateDialogShowcase() {
    DesignSystemTheme {
        DateDialog(initial = LocalDate.of(2026, 12, 24), onConfirm = {}, onDismiss = {}, onClear = {}, clearLabel = "No date")
    }
}

private val sheetOptions = listOf(
    "Holiday", "Dog", "Renovation", "Garden", "Car", "Wedding", "Bike", "Christmas", "Birthday", "Moving"
)

/** The content of a [MultiChoiceSheet] as it comes up from the bottom, with a search for the long list */
@Preview(name = "Multi choice sheet", showBackground = true, heightDp = 720)
@Preview(name = "Multi choice sheet, dark", showBackground = true, heightDp = 720, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun MultiChoiceSheetShowcase() {
    DesignSystemTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            SheetBody(
                actions = {
                    TextButton(onClick = {}) { Text("Cancel") }
                    Button(onClick = {}) { Text("OK") }
                },
                scrollable = false
            ) {
                MultiChoiceContent(
                    options = sheetOptions,
                    selected = setOf("Dog", "Garden"),
                    onToggle = {},
                    label = { it },
                    supportingText = { if (it == "Holiday") "Trip" else null },
                    intro = "More than eight options can be searched"
                )
            }
        }
    }
}

/** A sheet from the end in a wide window, e.g. a tablet or a phone in landscape */
@Preview(name = "Side sheet, wide", showBackground = true, widthDp = 840, heightDp = 480)
@Preview(name = "Side sheet, wide, dark", showBackground = true, widthDp = 840, heightDp = 480, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SideSheetShowcase() {
    DesignSystemTheme {
        Box(
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.32f))
            )
            SideSheetPanel(title = "Side sheet", onClose = {}, modifier = Modifier.align(Alignment.CenterEnd)) {
                SheetBody(
                    actions = { Button(onClick = {}) { Text("Apply") } },
                    scrollable = true
                ) {
                    sheetOptions.take(5).forEach { OptionRow(it, selected = it == "Dog", onClick = {}) }
                }
            }
        }
    }
}

/** The content of an [ActionSheet]: actions with icons, one opens a second level */
@Preview(name = "Action sheet", showBackground = true)
@Preview(name = "Action sheet, dark", showBackground = true, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ActionSheetShowcase() {
    DesignSystemTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainerLow)) {
            ActionSheetContent(
                actions = listOf(
                    SheetAction("Edit", icon = { rememberVectorPainter(Icons.Default.Edit) }),
                    SheetAction(
                        "Copy to",
                        icon = { rememberVectorPainter(Icons.Outlined.Folder) },
                        subActions = listOf(SheetAction("Checking"), SheetAction("Savings"))
                    ),
                    SheetAction(
                        "Delete",
                        icon = { rememberVectorPainter(Icons.Default.Delete) },
                        tint = MaterialTheme.colorScheme.error
                    ),
                ),
                title = "Rewe · −23,40 €",
                onDismiss = {}
            )
        }
    }
}

@Preview(name = "Single choice dialog", showBackground = true)
@Composable
private fun SingleChoiceDialogShowcase() {
    DesignSystemTheme {
        SingleChoiceDialog(
            title = "Single choice dialog",
            options = listOf("Automatic", "Insurance", "Own category"),
            selected = "Insurance",
            label = { it },
            onSelect = {},
            onDismiss = {},
            intro = "Picking one closes the dialog",
            trailing = { if (it == "Own category") IconButton(onClick = {}) { Icon(Icons.Default.Edit, contentDescription = null) } },
            footer = { DialogActionRow(Icons.Default.Add, "New option", onClick = {}) }
        )
    }
}

@Preview(name = "Pick dialog", showBackground = true)
@Composable
private fun PickDialogShowcase() {
    DesignSystemTheme {
        PickDialog(
            title = "Pick dialog",
            options = listOf("Netflix" to "12,99 €", "Spotify" to "10,99 €"),
            label = { it.first },
            onSelect = {},
            onDismiss = {},
            intro = "Picks one to act on",
            supportingText = { "Monthly" },
            value = { it.second }
        )
    }
}

@Preview(name = "Search", showBackground = true, heightDp = 640)
@Preview(name = "Search dark", showBackground = true, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun SearchShowcase() {
    DesignSystemTheme {
        Column(Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
            SearchTopBar(
                query = "Rewe", onQueryChange = {}, placeholder = "Search transactions",
                onClose = {}, closeLabel = "Close", clearLabel = "Clear"
            )
            SearchFilterRow {
                SearchFilterChip("Period", value = "October 2026", onClick = {}, icon = Icons.Outlined.Info)
                SearchFilterChip("Category", value = null, onClick = {}, icon = Icons.Outlined.Folder)
                SearchFilterChip("Amount", value = null, onClick = {})
            }
            SearchResultRow("23 transactions · −412,30 €", actionLabel = "Reset", onAction = {})
            SectionTitle("Content of a filter sheet")
            OptionRow("Groceries", selected = true, onClick = {}, leading = {
                IconTile { Icon(Icons.Outlined.Folder, contentDescription = null) }
            })
            OptionRow("Bakery", selected = false, onClick = {}, depth = 1)
            OptionRow("This month", selected = true, onClick = {}, isMultiple = false, supportingText = "1 – 31 October")
            SwitchRow("Exclude these", checked = false, onCheckedChange = {}, isFirst = true)
        }
    }
}

