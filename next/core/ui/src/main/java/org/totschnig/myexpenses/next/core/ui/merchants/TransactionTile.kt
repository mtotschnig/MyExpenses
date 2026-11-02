package org.totschnig.myexpenses.next.core.ui.merchants

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.totschnig.myexpenses.designsystem.PayeeTile
import org.totschnig.myexpenses.next.core.ui.CategoryIconContent
import org.totschnig.myexpenses.next.core.ui.CategoryIconTile
import org.totschnig.myexpenses.next.core.ui.icons.CatalogIconTile

/**
 * The tile of a transaction: the icon, logo or monogram of its payee, without payee the icon of
 * [CategoryIconTile]
 *
 * @param image of the payee, null without one or while its logo loads
 */
@Composable
fun TransactionTile(
    payee: String?,
    image: PayeeImage?,
    categoryIcon: String?,
    modifier: Modifier = Modifier,
    isLarge: Boolean = false,
) {
    if (image is PayeeImage.Icon) {
        CatalogIconTile(image.name, modifier, isLarge)
    } else {
        PayeeTile(payee?.takeIf { it.isNotBlank() }, (image as? PayeeImage.Logo)?.bitmap, modifier, isLarge) {
            CategoryIconContent(categoryIcon, isLarge)
        }
    }
}
