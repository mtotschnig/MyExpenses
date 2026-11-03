package org.totschnig.myexpenses.next

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.designsystem.ChoiceChips
import org.totschnig.myexpenses.designsystem.DateDialog
import org.totschnig.myexpenses.designsystem.FormPickerField
import org.totschnig.myexpenses.designsystem.FormTextField
import org.totschnig.myexpenses.designsystem.HintRow
import org.totschnig.myexpenses.designsystem.SingleChoiceDialog
import org.totschnig.myexpenses.designsystem.Spacing
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.core.ui.rememberDateFormatter
import org.totschnig.myexpenses.next.transactions.ChangedField
import org.totschnig.myexpenses.next.transactions.EditDraft
import org.totschnig.myexpenses.next.transactions.EditableTransaction
import org.totschnig.myexpenses.next.transactions.MethodOption
import org.totschnig.myexpenses.next.transactions.TransactionEditActions
import org.totschnig.myexpenses.next.transactions.TransactionEditState
import org.totschnig.myexpenses.next.transactions.differsInBankFields
import java.time.ZoneId
import java.time.format.FormatStyle

/**
 * The edit mode of the details: all fields of [transaction] at once, saved by the actions of the
 * sheet. A transaction from the bank import starts with a warning.
 *
 * @param currencySymbol after the amount
 */
@Composable
internal fun TransactionEditForm(
    transaction: EditableTransaction,
    draft: EditDraft,
    methods: List<MethodOption>,
    isAmountInvalid: Boolean,
    currencySymbol: String,
    onChange: (EditDraft) -> Unit,
) {
    val dateFormatter = rememberDateFormatter(FormatStyle.FULL)
    var showDate by rememberSaveable { mutableStateOf(false) }
    var showMethod by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.padding(bottom = Spacing.screen)) {
        if (transaction.isImported) {
            HintRow(stringResource(R.string.next_transaction_imported_warning), isFirst = true, isWarning = true)
        }
        Column(
            modifier = Modifier.padding(horizontal = Spacing.screen, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (transaction.isAmountEditable) {
                ChoiceChips(
                    options = listOf(false, true),
                    selected = draft.isIncome,
                    label = { stringResource(if (it) R.string.next_transaction_income else R.string.next_transaction_expense) },
                    onSelect = { onChange(draft.copy(isIncome = it)) }
                )
            }
            FormTextField(
                label = stringResource(R.string.next_transaction_amount),
                value = draft.amountText,
                onValueChange = { onChange(draft.copy(amountText = it)) },
                keyboardType = KeyboardType.Decimal,
                enabled = transaction.isAmountEditable,
                isError = isAmountInvalid,
                supportingText = when {
                    isAmountInvalid -> stringResource(R.string.next_transaction_amount_invalid)
                    !transaction.isAmountEditable -> stringResource(R.string.next_transaction_amount_foreign)
                    else -> null
                },
                trailing = { Text(currencySymbol) }
            )
            FormTextField(
                label = stringResource(R.string.next_transaction_payee),
                value = draft.payee,
                onValueChange = { onChange(draft.copy(payee = it)) }
            )
            FormPickerField(
                label = stringResource(R.string.next_transaction_date),
                value = dateFormatter.format(draft.day),
                onClick = { showDate = true }
            )
            if (methods.isNotEmpty() || draft.methodId != null) {
                FormPickerField(
                    label = stringResource(R.string.next_transaction_method),
                    value = methods.find { it.id == draft.methodId }?.label
                        ?: stringResource(R.string.next_transaction_no_method),
                    onClick = { showMethod = true }
                )
            }
            FormTextField(
                label = stringResource(R.string.next_transaction_comment),
                value = draft.comment,
                onValueChange = { onChange(draft.copy(comment = it)) },
                singleLine = false
            )
        }
    }
    if (showDate) {
        DateDialog(
            initial = draft.day,
            onConfirm = {
                onChange(draft.copy(day = it))
                showDate = false
            },
            onDismiss = { showDate = false }
        )
    }
    if (showMethod) {
        MethodDialog(methods, draft.methodId, onSelect = { onChange(draft.copy(methodId = it)) }, onDismiss = { showMethod = false })
    }
}

/** Picks one of [methods] or none */
@Composable
internal fun MethodDialog(
    methods: List<MethodOption>,
    selectedId: Long?,
    onSelect: (Long?) -> Unit,
    onDismiss: () -> Unit,
) {
    val none = stringResource(R.string.next_transaction_no_method)
    SingleChoiceDialog(
        title = stringResource(R.string.next_transaction_method),
        options = listOf<MethodOption?>(null) + methods,
        selected = methods.find { it.id == selectedId },
        label = { it?.label ?: none },
        onSelect = {
            onSelect(it?.id)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

/**
 * Offers to undo the last change made in the details, in the snackbar of the sheet. For a
 * transaction from the bank import it also tells when the change no longer matches the bank.
 */
@Composable
internal fun UndoSnackbar(state: TransactionEditState, actions: TransactionEditActions, host: SnackbarHostState) {
    val undo = state.undo ?: return
    val saved = stringResource(
        when (undo.field) {
            ChangedField.COMMENT -> R.string.next_transaction_saved_comment
            ChangedField.DATE -> R.string.next_transaction_saved_date
            ChangedField.METHOD -> R.string.next_transaction_saved_method
            ChangedField.ALL -> R.string.next_transaction_saved_all
        }
    )
    val current = state.transaction?.takeIf { it.id == undo.id }
    val differs = current != null && current.isImported &&
            undo.previous.differsInBankFields(current.values, ZoneId.systemDefault())
    val message = if (differs) stringResource(R.string.next_transaction_saved_differs, saved) else saved
    val undoLabel = stringResource(R.string.next_transaction_undo)
    LaunchedEffect(undo) {
        val result = host.showSnackbar(message, actionLabel = undoLabel, duration = SnackbarDuration.Short)
        if (result == SnackbarResult.ActionPerformed) actions.undo() else actions.undoShown()
    }
}
