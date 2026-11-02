package org.totschnig.myexpenses.next

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.unit.TextUnit
import org.totschnig.myexpenses.viewmodel.data.FontAwesomeIcons
import org.totschnig.myexpenses.compose.Icon as FontIcon
import org.totschnig.myexpenses.next.core.ui.FontIcons

/** The Font Awesome icons of the app for the features of the new UI */
object AppFontIcons : FontIcons {
    @Composable
    override fun Icon(name: String, modifier: Modifier, size: TextUnit, color: Color) {
        FontIcon(name, modifier, size = size, color = color)
    }

    @Composable
    override fun labels(): Map<String, String> {
        val resources = LocalResources.current
        return remember(resources) { FontAwesomeIcons.mapValues { (_, info) -> resources.getString(info.label) } }
    }
}
