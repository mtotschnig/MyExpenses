package org.totschnig.myexpenses.compose.main

import android.annotation.SuppressLint
import android.content.Context
import android.view.MenuInflater
import androidx.annotation.MenuRes
import androidx.appcompat.view.SupportMenuInflater
import androidx.appcompat.view.menu.MenuBuilder
import androidx.core.view.forEach
import com.google.accompanist.drawablepainter.rememberDrawablePainter
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.SubMenuEntry
import org.totschnig.myexpenses.compose.UiText
import android.view.Menu as AMenu

/**
 * Parses an XML menu resource and converts its items into a list of MenuAction data objects.
 * This allows reusing XML menu definitions in Compose UIs.
 */
@SuppressLint("RestrictedApi")
fun parseMenu(
    context: Context,
    @MenuRes menuRes: Int,
    onPrepareMenuItem: (Int) -> Boolean = { true },
    onMenuItemClicked: (Int) -> Unit,
): Menu {
    val menu = MenuBuilder(context)
    val inflater: MenuInflater = SupportMenuInflater(context)
    inflater.inflate(menuRes, menu)
    return menu.map(context, onPrepareMenuItem, onMenuItemClicked)
}

private fun AMenu.map(
    context: Context,
    onPrepareMenuItem: (Int) -> Boolean,
    onMenuItemClicked: (Int) -> Unit,
): Menu = buildList {
    this@map.forEach { menuItem ->
        if (onPrepareMenuItem(menuItem.itemId)) {
            add(
                menuItem.subMenu?.let {
                    SubMenuEntry(
                        label = UiText.StringValue(menuItem.title.toString()),
                        icon = {
                            rememberDrawablePainter(menuItem.icon)
                        },
                        subMenu = it.map(context, onPrepareMenuItem, onMenuItemClicked)
                    )
                } ?: MenuEntry(
                    command = context.resources.getResourceName(menuItem.itemId),
                    label = UiText.StringValue(menuItem.title.toString()),
                    icon = {
                        rememberDrawablePainter(menuItem.icon)
                    }
                ) { onMenuItemClicked(menuItem.itemId) }
            )
        }
    }
}