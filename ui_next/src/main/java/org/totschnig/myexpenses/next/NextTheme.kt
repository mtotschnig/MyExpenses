package org.totschnig.myexpenses.next

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.Colors
import org.totschnig.myexpenses.compose.LocalColors
import org.totschnig.myexpenses.compose.LocalCurrencyFormatter
import org.totschnig.myexpenses.designsystem.theme.Contrast
import org.totschnig.myexpenses.designsystem.theme.LocalSemanticColors
import org.totschnig.myexpenses.designsystem.theme.SemanticColors
import org.totschnig.myexpenses.designsystem.theme.colorScheme
import org.totschnig.myexpenses.designsystem.theme.semanticColors
import org.totschnig.myexpenses.next.core.ui.LocalAmountFormatter
import org.totschnig.myexpenses.next.core.ui.LocalFontIcons

/**
 * Theme of the new UI: [AppTheme] with the colors of the design system. The semantic colors
 * are also handed to [LocalColors], so screens that still use them look the same.
 */
@Composable
fun NextTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val contrast = Contrast.current()
    AppTheme(darkTheme = darkTheme, colorScheme = colorScheme(darkTheme, contrast)) {
        val semanticColors = semanticColors(darkTheme, contrast)
        CompositionLocalProvider(
            LocalSemanticColors provides semanticColors,
            LocalColors provides semanticColors.toColors(),
            LocalAmountFormatter provides LocalCurrencyFormatter.current.toAmountFormatter(),
            LocalFontIcons provides AppFontIcons,
            *NextFeatures.locals,
            content = content
        )
    }
}

private fun SemanticColors.toColors() = Colors(income = income, expense = expense, transfer = transfer)
