package org.totschnig.myexpenses.designsystem.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Colors with a meaning beyond the Material color scheme, see [semanticColors]
 */
@Immutable
data class SemanticColors(
    val income: Color,
    val expense: Color,
    val transfer: Color,
    /** Something needs attention soon, e.g. a budget that is nearly used up */
    val warning: Color,
) {
    /**
     * Color of an amount: income if positive, expense if negative, unchanged if zero
     */
    fun amountColor(sign: Int) = when {
        sign > 0 -> income
        sign < 0 -> expense
        else -> Color.Unspecified
    }
}

/**
 * Semantic colors matching [colorScheme] for the same parameters. Income and expense are custom
 * colors exported from Material Theme Builder, transfer is the secondary color of the scheme.
 */
fun semanticColors(darkTheme: Boolean, contrast: Contrast): SemanticColors {
    val transfer = colorScheme(darkTheme, contrast).secondary
    return when (contrast) {
        Contrast.High -> if (darkTheme)
            SemanticColors(incomeDarkHighContrast, expenseDarkHighContrast, transfer, warningDarkHighContrast)
        else SemanticColors(incomeLightHighContrast, expenseLightHighContrast, transfer, warningLightHighContrast)

        Contrast.Medium -> if (darkTheme)
            SemanticColors(incomeDarkMediumContrast, expenseDarkMediumContrast, transfer, warningDarkMediumContrast)
        else SemanticColors(incomeLightMediumContrast, expenseLightMediumContrast, transfer, warningLightMediumContrast)

        Contrast.Standard -> if (darkTheme)
            SemanticColors(incomeDark, expenseDark, transfer, warningDark)
        else SemanticColors(incomeLight, expenseLight, transfer, warningLight)
    }
}

// Orange tones that keep at least 3:1 to the surfaces and tracks they are drawn on
private val warningLight = Color(0xFF8B5000)
private val warningLightMediumContrast = Color(0xFF6B3D00)
private val warningLightHighContrast = Color(0xFF4F2C00)
private val warningDark = Color(0xFFFFB870)
private val warningDarkMediumContrast = Color(0xFFFFCF9E)
private val warningDarkHighContrast = Color(0xFFFFE9D4)

val LocalSemanticColors = staticCompositionLocalOf { semanticColors(darkTheme = false, Contrast.Standard) }
