package org.totschnig.myexpenses.next

import org.totschnig.myexpenses.next.core.ui.merchants.TransactionTile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.designsystem.AdaptiveSheet
import org.totschnig.myexpenses.designsystem.ChipRow
import org.totschnig.myexpenses.designsystem.DetailCard
import org.totschnig.myexpenses.designsystem.InfoRow
import org.totschnig.myexpenses.designsystem.Spacing
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.time.format.FormatStyle
import org.totschnig.myexpenses.next.core.ui.merchants.PayeeImage
import org.totschnig.myexpenses.next.core.common.NO_NAME
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.rememberDateFormatter

/**
 * Details of a transaction in a sheet above the list, laid out like the details of a contract:
 * from the bottom, first up to half the height, or from the side in wide windows.
 * The actions of the transaction (edit, delete, …) are in the menu next to the payee.
 *
 * Only the basics for now, the details of transactions are to be designed further.
 *
 * @param menu actions of the transaction, null if it cannot be changed here
 * @param categoryChip shown below the title instead of the row with the category, to change it,
 * labelled "Wofür"
 * @param tagChips shown below the category chip, to set groups and markers, labelled by itself
 * @param content added below the details, e.g. the contract the transaction belongs to
 */
@Composable
fun TransactionDetailSheet(
    transaction: Transaction2,
    menu: (() -> Menu)?,
    onDismiss: () -> Unit,
    categoryChip: (@Composable (Modifier) -> Unit)? = null,
    tagChips: (@Composable (Modifier) -> Unit)? = null,
    image: PayeeImage? = null,
    onImageClick: (() -> Unit)? = null,
    content: @Composable () -> Unit = {},
) {
    AdaptiveSheet(onDismiss = onDismiss, skipPartiallyExpanded = false) {
        TransactionDetails(transaction, menu, categoryChip, tagChips, image, onImageClick, content)
    }
}

@Composable
private fun TransactionDetails(
    transaction: Transaction2,
    menu: (() -> Menu)?,
    categoryChip: (@Composable (Modifier) -> Unit)?,
    tagChips: (@Composable (Modifier) -> Unit)?,
    image: PayeeImage?,
    onImageClick: (() -> Unit)?,
    content: @Composable () -> Unit,
) {
    val dateFormatter = rememberDateFormatter(FormatStyle.FULL)
    Column(Modifier.padding(start = Spacing.screen, end = Spacing.screen, bottom = Spacing.screen)) {
        DetailCard {
            Header(transaction, image, onImageClick) {
                if (menu != null) {
                    val showMenu = rememberSaveable { mutableStateOf(false) }
                    IconButton(onClick = { showMenu.value = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = stringResource(R.string.next_transaction_actions))
                    }
                    MenuActionSheet(showMenu, menu, title = transaction.party?.displayName)
                }
            }
            if (categoryChip != null || tagChips != null) {
                Column(
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    categoryChip?.let { chip ->
                        ChipRow(stringResource(R.string.next_details_category)) { chip(Modifier) }
                    }
                    tagChips?.invoke(Modifier)
                }
            }
            InfoRow(stringResource(R.string.next_transaction_date), dateFormatter.format(transaction.date))
            transaction.accountLabel?.takeIf { it.isNotBlank() }?.let {
                InfoRow(stringResource(R.string.next_transaction_account), it)
            }
            if (categoryChip == null) transaction.categoryPath?.let {
                InfoRow(stringResource(R.string.next_contracts_transaction_category), it)
            }
            transaction.methodLabel?.let {
                InfoRow(stringResource(R.string.next_transaction_method), it)
            }
            transaction.comment?.takeIf { it.isNotBlank() }?.let {
                InfoRow(stringResource(R.string.next_transaction_comment), it)
            }
        }
        content()
    }
}

@Composable
private fun Header(
    transaction: Transaction2,
    image: PayeeImage?,
    onImageClick: (() -> Unit)?,
    actions: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TransactionTile(
            transaction.party?.displayName,
            image,
            transaction.icon,
            modifier = if (onImageClick != null) Modifier.clickable(
                onClickLabel = stringResource(R.string.next_payee_image_change),
                onClick = onImageClick
            ) else Modifier,
            isLarge = true
        )
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(
                transaction.party?.name ?: transaction.categoryPath ?: NO_NAME,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            SignedAmount(transaction.displayAmount, style = MaterialTheme.typography.titleMedium)
        }
        actions()
    }
}
