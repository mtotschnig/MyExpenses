package org.totschnig.myexpenses.next

import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.remember
import org.totschnig.myexpenses.compose.IActionMenuEntry
import org.totschnig.myexpenses.compose.LocalTracker
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.SubMenuEntry
import org.totschnig.myexpenses.designsystem.ActionSheet
import org.totschnig.myexpenses.designsystem.SheetAction
import org.totschnig.myexpenses.util.tracking.Tracker

/**
 * The actions of [menu] in an [ActionSheet] while [expanded], e.g. those of a transaction.
 * Sub menus open as a second level.
 *
 * @param menu built when the sheet opens, so that it reflects the current state
 * @param title above the actions, e.g. what they apply to
 */
@Composable
internal fun MenuActionSheet(expanded: MutableState<Boolean>, menu: () -> Menu, title: String? = null) {
    if (!expanded.value) return
    val tracker = LocalTracker.current
    val entries = remember { menu() }
    ActionSheet(
        actions = entries.toSheetActions(tracker),
        onDismiss = { expanded.value = false },
        title = title
    )
}

@Composable
private fun Menu.toSheetActions(tracker: Tracker): List<SheetAction> = map { entry ->
    SheetAction(
        label = entry.label.asString(),
        icon = entry.icon,
        tint = entry.tint,
        testTag = entry.command,
        contentDescription = entry.contentDescription,
        subActions = (entry as? SubMenuEntry)?.subMenu?.toSheetActions(tracker).orEmpty(),
        onClick = {
            entry.command?.let(tracker::trackCommand)
            (entry as? IActionMenuEntry)?.action?.invoke()
        }
    )
}
