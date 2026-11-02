package org.totschnig.myexpenses.next.core.ui.icons

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import org.totschnig.myexpenses.designsystem.IconTile
import org.totschnig.myexpenses.next.core.ui.LocalFontIcons

/**
 * An icon of Font Awesome by its name, in the colour of icons on tiles
 *
 * @param isLarge in the header of details, small in lists
 */
@Composable
fun CatalogIcon(name: String, modifier: Modifier = Modifier, isLarge: Boolean = false) {
    LocalFontIcons.current.Icon(name, modifier, if (isLarge) 28.sp else 20.sp, MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A [CatalogIcon] on an [IconTile] */
@Composable
fun CatalogIconTile(name: String, modifier: Modifier = Modifier, isLarge: Boolean = false) {
    IconTile(modifier, isLarge) { CatalogIcon(name, isLarge = isLarge) }
}

