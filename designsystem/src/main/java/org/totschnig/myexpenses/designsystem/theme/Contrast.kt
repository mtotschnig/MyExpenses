package org.totschnig.myexpenses.designsystem.theme

import android.app.UiModeManager
import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

/**
 * Contrast level selected in the system settings
 */
enum class Contrast {
    Standard, Medium, High;

    companion object {
        /**
         * Available from Android 14 on, before [Standard]
         */
        @Composable
        fun current(): Contrast {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return Standard
            val contrast = LocalContext.current.getSystemService(UiModeManager::class.java)?.contrast ?: 0f
            return when {
                contrast >= 1f -> High
                contrast >= 0.5f -> Medium
                else -> Standard
            }
        }
    }
}
