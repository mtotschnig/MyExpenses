package org.totschnig.myexpenses.next.feature.merchants

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.totschnig.myexpenses.designsystem.IconPickerOption
import org.totschnig.myexpenses.designsystem.IconTile
import org.totschnig.myexpenses.designsystem.PayeeTile
import org.totschnig.myexpenses.next.core.ui.icons.CatalogIconPicker
import org.totschnig.myexpenses.next.core.common.icons.IconCatalog
import org.totschnig.myexpenses.next.core.common.R as NextR
import org.totschnig.myexpenses.next.core.ui.merchants.PayeeImage
import org.totschnig.myexpenses.next.data.merchants.Payee

/**
 * Lets the user choose what [payee] is shown with: an icon, the logo of a web address, or, back
 * to automatic, the logo of its merchant or the first letter of its name
 *
 * @param categoryPath of the transaction it was opened from, to suggest icons
 * @param categoryIcon of that transaction, suggested first
 */
@Composable
fun PayeeImageDialog(
    logos: MerchantLogos,
    payee: Payee,
    categoryPath: String?,
    categoryIcon: String?,
    onDismiss: () -> Unit,
) {
    val payeeId = payee.id ?: return
    val chosen = logos.chosenIcons.collectAsStateWithLifecycle().value[payeeId]
    val entered = logos.enteredDomains.collectAsStateWithLifecycle().value[payeeId]
    val logo = (logos.images.collectAsStateWithLifecycle().value[payeeId] as? PayeeImage.Logo)?.bitmap
    var showAddress by rememberSaveable { mutableStateOf(false) }
    CatalogIconPicker(
        title = stringResource(NextR.string.next_icons_title, payee.displayName),
        selected = chosen,
        suggestions = listOfNotNull(categoryIcon) +
                IconCatalog.suggest(payee.displayName, *categoryPath?.split('>')?.asReversed()?.toTypedArray().orEmpty()),
        onSelect = { icon ->
            if (icon == null) logos.useAutomatic(payeeId) else logos.setIcon(payeeId, icon)
            onDismiss()
        },
        onDismiss = onDismiss,
        isAutomatic = chosen == null && entered == null,
        automatic = { PayeeTile(payee.displayName, logo.takeIf { entered == null }) {} },
        automaticHint = stringResource(NextR.string.next_payee_image_automatic),
        header = {
            IconPickerOption(
                title = stringResource(NextR.string.next_payee_image_address),
                supportingText = entered ?: stringResource(NextR.string.next_payee_image_address_hint),
                isSelected = entered != null,
                onClick = { showAddress = true }
            ) {
                if (entered != null && logo != null) PayeeTile(payee.displayName, logo) {}
                else IconTile {
                    Icon(Icons.Outlined.Language, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    )
    // Above the picker, which stays open to show the logo as chosen
    if (showAddress) {
        PayeeLogoDialog(logos, payee) { showAddress = false }
    }
}
