package org.totschnig.myexpenses.next.core.ui.merchants

import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.ImageBitmap

/** What is shown for each payee, by id, see `MerchantLogos.images`; none in previews */
val LocalPayeeImages = compositionLocalOf<(payeeId: Long) -> PayeeImage?> { { null } }

/** What a payee is shown with in place of the first letter of its name */
sealed interface PayeeImage {
    data class Logo(val bitmap: ImageBitmap) : PayeeImage

    /** @param name of an icon of the [org.totschnig.myexpenses.next.icons.IconCatalog] */
    data class Icon(val name: String) : PayeeImage
}
