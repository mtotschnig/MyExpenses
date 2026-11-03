package org.totschnig.myexpenses.next

import org.totschnig.myexpenses.next.core.ui.merchants.TransactionTile
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.designsystem.AdaptiveSheet
import org.totschnig.myexpenses.designsystem.ChipRow
import org.totschnig.myexpenses.designsystem.DateDialog
import org.totschnig.myexpenses.designsystem.InfoRow
import org.totschnig.myexpenses.designsystem.Spacing
import org.totschnig.myexpenses.designsystem.TextDialog
import org.totschnig.myexpenses.next.transactions.EditableTransaction
import org.totschnig.myexpenses.next.transactions.TransactionEditActions
import org.totschnig.myexpenses.next.transactions.TransactionEditState
import org.totschnig.myexpenses.next.transactions.isEditableInDetails
import org.totschnig.myexpenses.viewmodel.data.Transaction2
import java.time.Instant
import java.time.ZoneId
import java.time.format.FormatStyle
import org.totschnig.myexpenses.next.core.ui.merchants.PayeeImage
import org.totschnig.myexpenses.next.core.common.NO_NAME
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.rememberDateFormatter

/**
 * Changing the opened transaction from its details
 *
 * @param onEditClassic opens the classic form, for what the details cannot change, e.g. a split
 */
class DetailsEditing(
    val state: TransactionEditState,
    val actions: TransactionEditActions,
    val onEditClassic: () -> Unit,
)

/**
 * Details of a transaction in a sheet above the list, the rows straight on the sheet:
 * from the bottom, first up to half the height, or from the side in wide windows.
 * The pencil next to the payee opens the edit mode with all fields, date, payment method and
 * note can also be changed one by one by tapping their row; the other actions of the transaction
 * (delete, copy, …) are in the menu.
 *
 * @param menu actions of the transaction, null if it cannot be changed here
 * @param categoryChip shown below the title instead of the row with the category, to change it,
 * labelled "Wofür"
 * @param tagChips shown below the category chip, to set groups and markers, labelled by itself
 * @param editing changes the transaction, null where it cannot be changed, e.g. in read-only lists
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
    editing: DetailsEditing? = null,
    content: @Composable () -> Unit = {},
) {
    val editable = editing?.state?.transaction?.takeIf { it.id == transaction.id }
    val draft = editing?.state?.draft?.takeIf { it.id == transaction.id && editable != null }
    val snackbar = remember { SnackbarHostState() }
    if (editing != null) UndoSnackbar(editing.state, editing.actions, snackbar)
    AdaptiveSheet(
        onDismiss = {
            editing?.actions?.cancelEditing()
            onDismiss()
        },
        title = if (draft != null) stringResource(R.string.next_transaction_edit_title) else null,
        actions = if (editing != null && draft != null) {
            {
                TextButton(onClick = editing.actions::cancelEditing) {
                    Text(stringResource(R.string.next_transaction_cancel))
                }
                Button(onClick = editing.actions::saveDraft) {
                    Text(stringResource(R.string.next_transaction_save))
                }
            }
        } else null,
        skipPartiallyExpanded = false,
        snackbarHostState = snackbar,
    ) {
        if (editing != null && editable != null && draft != null) {
            TransactionEditForm(
                transaction = editable,
                draft = draft,
                methods = editing.state.methods,
                isAmountInvalid = editing.state.isAmountInvalid,
                currencySymbol = (transaction.amount ?: transaction.displayAmount).currencyUnit.symbol,
                onChange = editing.actions::changeDraft
            )
        } else {
            TransactionDetails(transaction, menu, categoryChip, tagChips, image, onImageClick, editing, content)
        }
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
    editing: DetailsEditing?,
    content: @Composable () -> Unit,
) {
    // What the details can change, null while it loads or if they cannot change it
    val editable = editing?.state?.transaction?.takeIf { it.id == transaction.id && menu != null }
    // Straight on the sheet: a card would highlight the details, but in the sheet they are all
    // there is. The rows pad themselves, header, chips and the added content are padded here.
    Column(Modifier.padding(bottom = Spacing.screen)) {
        Header(transaction, image, onImageClick) {
            if (editing != null && menu != null) {
                // Simple transactions wait until it is known whether the details can change them
                val isLoaded = editing.state.loadedId == transaction.id
                IconButton(
                    onClick = { if (editable != null) editing.actions.startEditing() else editing.onEditClassic() },
                    enabled = !transaction.isEditableInDetails() || isLoaded
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = stringResource(R.string.next_transaction_edit))
                }
            }
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
                modifier = Modifier.padding(start = Spacing.screen, end = Spacing.screen, bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                categoryChip?.let { chip ->
                    ChipRow(stringResource(R.string.next_details_category)) { chip(Modifier) }
                }
                tagChips?.invoke(Modifier)
            }
        }
        DetailRows(transaction, categoryChip == null, editable, editing)
        Column(Modifier.padding(horizontal = Spacing.screen)) {
            content()
        }
    }
}

/**
 * Date, account, category, payment method and note. With [editable] date, method and note show
 * what was just saved and are changed by tapping them, an empty method or note offers to add one.
 */
@Composable
private fun DetailRows(
    transaction: Transaction2,
    withCategory: Boolean,
    editable: EditableTransaction?,
    editing: DetailsEditing?,
) {
    val dateFormatter = rememberDateFormatter(FormatStyle.FULL)
    val addLabel = stringResource(R.string.next_transaction_add)
    val addColor = MaterialTheme.colorScheme.primary
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showMethod by rememberSaveable { mutableStateOf(false) }
    var showComment by rememberSaveable { mutableStateOf(false) }
    val change = editing?.actions?.takeIf { editable != null }
    val day = editable?.values?.date?.let { Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).toLocalDate() }
        ?: transaction.date.toLocalDate()
    InfoRow(
        stringResource(R.string.next_transaction_date),
        dateFormatter.format(day),
        onClick = change?.let { { showDate = true } }
    )
    transaction.accountLabel?.takeIf { it.isNotBlank() }?.let {
        InfoRow(stringResource(R.string.next_transaction_account), it)
    }
    if (withCategory) transaction.categoryPath?.let {
        InfoRow(stringResource(R.string.next_contracts_transaction_category), it)
    }
    val methods = editing?.state?.methods.orEmpty()
    val methodLabel = if (editable == null) transaction.methodLabel
    else editable.values.methodId?.let { id -> methods.find { it.id == id }?.label ?: transaction.methodLabel }
    if (methodLabel != null || (change != null && methods.isNotEmpty())) {
        InfoRow(
            stringResource(R.string.next_transaction_method),
            methodLabel ?: addLabel,
            valueColor = if (methodLabel == null) addColor else Color.Unspecified,
            onClick = change?.let { { showMethod = true } }
        )
    }
    val comment = (if (editable != null) editable.values.comment else transaction.comment)?.takeIf { it.isNotBlank() }
    if (comment != null || change != null) {
        InfoRow(
            stringResource(R.string.next_transaction_comment),
            comment ?: addLabel,
            valueColor = if (comment == null) addColor else Color.Unspecified,
            onClick = change?.let { { showComment = true } }
        )
    }
    if (change == null || editable == null) return
    if (showDate) {
        DateDialog(
            initial = day,
            onConfirm = {
                change.changeDay(it)
                showDate = false
            },
            onDismiss = { showDate = false }
        )
    }
    if (showMethod) {
        MethodDialog(methods, editable.values.methodId, onSelect = change::changeMethod, onDismiss = { showMethod = false })
    }
    if (showComment) {
        TextDialog(
            title = stringResource(R.string.next_transaction_comment),
            initialText = comment.orEmpty(),
            onConfirm = {
                change.changeComment(it)
                showComment = false
            },
            onDismiss = { showComment = false }
        )
    }
}

@Composable
private fun Header(
    transaction: Transaction2,
    image: PayeeImage?,
    onImageClick: (() -> Unit)?,
    actions: @Composable () -> Unit,
) {
    Column(
        // Little space at the top, the drag handle of the sheet already keeps its distance
        modifier = Modifier.padding(start = Spacing.screen, end = Spacing.screen, top = 0.dp, bottom = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // The amount first, centered above payee and menu and about twice as large as the name
        SignedAmount(transaction.displayAmount, style = MaterialTheme.typography.displayMedium)
        Spacer(Modifier.height(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
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
            Text(
                transaction.party?.name ?: transaction.categoryPath ?: NO_NAME,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            actions()
        }
    }
}
