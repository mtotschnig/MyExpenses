package org.totschnig.myexpenses.compose

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

class Colors(
    val income: Color,
    val expense: Color
)

val LocalColors = compositionLocalOf<Colors> { throw IllegalStateException("Colors not initialized") }
