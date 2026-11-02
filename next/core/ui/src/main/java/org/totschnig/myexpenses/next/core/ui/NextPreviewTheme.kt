package org.totschnig.myexpenses.next.core.ui

import androidx.compose.runtime.Composable
import org.totschnig.myexpenses.designsystem.theme.DesignSystemTheme

/**
 * The theme of the new UI for previews, where the theme of the app is not available since it needs
 * the application. Amounts are formatted with [SimpleAmountFormatter].
 */
@Composable
fun NextPreviewTheme(content: @Composable () -> Unit) {
    DesignSystemTheme(content = content)
}
