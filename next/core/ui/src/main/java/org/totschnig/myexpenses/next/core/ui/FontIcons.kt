package org.totschnig.myexpenses.next.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit

/**
 * The icons of categories and of the icon catalog: glyphs of a font of the app, named like
 * "shield-halved". The app hands them in with [LocalFontIcons].
 */
interface FontIcons {
    @Composable
    fun Icon(name: String, modifier: Modifier, size: TextUnit, color: Color)

    /** The label of each icon by name, e.g. to search for icons */
    @Composable
    fun labels(): Map<String, String>
}

/** Used where the app provides none, e.g. in previews: draws nothing */
private object NoFontIcons : FontIcons {
    @Composable
    override fun Icon(name: String, modifier: Modifier, size: TextUnit, color: Color) {}

    @Composable
    override fun labels(): Map<String, String> = emptyMap()
}

val LocalFontIcons = staticCompositionLocalOf<FontIcons> { NoFontIcons }
