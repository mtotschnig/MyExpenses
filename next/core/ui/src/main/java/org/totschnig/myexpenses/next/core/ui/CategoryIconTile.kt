package org.totschnig.myexpenses.next.core.ui

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ReceiptLong
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.designsystem.IconTile

/**
 * The icon of a category on an [IconTile], a receipt for transactions and contracts without one
 *
 * @param isLarge in the header of details, small in lists
 */
@Composable
fun CategoryIconTile(icon: String?, modifier: Modifier = Modifier, isLarge: Boolean = false) {
    IconTile(modifier, isLarge) { CategoryIconContent(icon, isLarge) }
}

/** The icon of a category, a receipt without one */
@Composable
fun CategoryIconContent(icon: String?, isLarge: Boolean) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    if (icon != null) {
        LocalFontIcons.current.Icon(icon, Modifier, if (isLarge) 28.sp else 20.sp, color)
    } else {
        Icon(
            Icons.AutoMirrored.Filled.ReceiptLong,
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(if (isLarge) 28.dp else 20.dp)
        )
    }
}
