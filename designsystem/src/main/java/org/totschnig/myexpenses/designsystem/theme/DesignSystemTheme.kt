package org.totschnig.myexpenses.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

/**
 * Colors of the design system: the [colorScheme] and the matching [semanticColors].
 * Used where the theme of the app is not available, e.g. in previews and the fixture collector.
 * Text and icons without a color of their own are drawn in onSurface, as in the theme of the app,
 * since pages draw their background without a Surface, e.g. [org.totschnig.myexpenses.designsystem.DetailScaffold].
 */
@Composable
fun DesignSystemTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    contrast: Contrast = Contrast.current(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(colorScheme = colorScheme(darkTheme, contrast)) {
        CompositionLocalProvider(
            LocalSemanticColors provides semanticColors(darkTheme, contrast),
            LocalContentColor provides MaterialTheme.colorScheme.onSurface,
            content = content
        )
    }
}
