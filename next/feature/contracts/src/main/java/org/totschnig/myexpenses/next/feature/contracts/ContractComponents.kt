package org.totschnig.myexpenses.next.feature.contracts

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import org.totschnig.myexpenses.designsystem.ConfirmDialog
import org.totschnig.myexpenses.designsystem.DestructiveTextButton
import org.totschnig.myexpenses.designsystem.FormDialog
import org.totschnig.myexpenses.designsystem.NameDialog
import org.totschnig.myexpenses.next.core.common.R
import org.totschnig.myexpenses.next.data.contracts.BuiltInArea
import org.totschnig.myexpenses.next.data.contracts.Contract
import org.totschnig.myexpenses.next.data.contracts.ContractArea
import org.totschnig.myexpenses.next.data.contracts.CustomArea
import org.totschnig.myexpenses.next.data.contracts.icon
import org.totschnig.myexpenses.next.data.contracts.labelRes

/**
 * Name of the category as shown to the user, null stands for no category
 */
@Composable
internal fun ContractArea?.label(): String = when (this) {
    is BuiltInArea -> stringResource(labelRes)
    is CustomArea -> name
    null -> stringResource(R.string.next_contracts_area_none)
}

/**
 * Renames or deletes a category created by the user. Deleting asks for confirmation.
 */
@Composable
internal fun EditAreaDialog(
    area: CustomArea,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(area.name) }
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.next_contracts_delete_area),
            text = stringResource(R.string.next_contracts_delete_area_confirm, area.name),
            confirmLabel = stringResource(R.string.next_contracts_delete_area),
            onConfirm = onDelete,
            onDismiss = { confirmDelete = false }
        )
        return
    }
    FormDialog(
        title = stringResource(R.string.next_contracts_edit_area),
        onConfirm = { onRename(name) },
        onDismiss = onDismiss,
        confirmEnabled = name.isNotBlank()
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text(stringResource(R.string.next_contracts_area)) },
            singleLine = true
        )
        DestructiveTextButton(
            stringResource(R.string.next_contracts_delete_area),
            onClick = { confirmDelete = true },
            icon = Icons.Default.Delete
        )
    }
}

/** Renames [contract]; an empty name goes back to the detected one */
@Composable
internal fun RenameContractDialog(contract: Contract, actions: ContractActions, onDismiss: () -> Unit) {
    NameDialog(
        title = stringResource(R.string.next_contracts_rename),
        initialName = contract.displayName,
        placeholder = contract.name,
        allowBlank = true,
        onConfirm = {
            actions.rename(contract, it)
            onDismiss()
        },
        onDismiss = onDismiss
    )
}

