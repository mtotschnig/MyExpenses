package org.totschnig.myexpenses.dialog

import android.os.Bundle
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.RemapHandler.Companion.KEY_COLUMN
import org.totschnig.myexpenses.activity.RemapHandler.Companion.KEY_UPDATE_VALUE_DATE
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.Companion.KEY_COMMAND_POSITIVE
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment.ConfirmationDialogListener
import org.totschnig.myexpenses.provider.KEY_DATE

class RemapDialogFragment : ComposeBaseDialogFragment3() {

    override val title: CharSequence
        get() = requireArguments().getString(KEY_TITLE_STRING) ?: ""

    @Composable
    override fun ColumnScope.MainContent() {
        val column = requireArguments().getString(KEY_COLUMN) ?: ""
        val message = requireArguments().getString(KEY_MESSAGE) ?: ""
        var shouldClone by rememberSaveable { mutableStateOf(value = false) }
        var shouldUpdateValueDate by rememberSaveable { mutableStateOf(value = false) }

        Column(modifier = Modifier.fillMaxWidth()) {
            if (message.isNotEmpty()) {
                Text(
                    text = message,
                    modifier = Modifier.padding(bottom = 16.dp),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = shouldClone,
                        onValueChange = { shouldClone = it },
                        role = Role.Checkbox,
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = shouldClone,
                    onCheckedChange = null,
                )
                Text(
                    text = stringResource(id = R.string.menu_clone_transaction),
                    modifier = Modifier.padding(start = 12.dp),
                )
            }

            if (column == KEY_DATE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = shouldUpdateValueDate,
                            onValueChange = { shouldUpdateValueDate = it },
                            role = Role.Checkbox,
                        )
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = shouldUpdateValueDate,
                        onCheckedChange = null,
                    )
                    Text(
                        text = stringResource(id = R.string.remap_update_value_date),
                        modifier = Modifier.padding(start = 12.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            ButtonRow {
                TextButton(
                    onClick = {
                        (activity as? ConfirmationDialogListener)?.onDismissOrCancel()
                        dismiss()
                    },
                ) {
                    Text(stringResource(id = android.R.string.cancel))
                }
                TextButton(
                    onClick = {
                        val args = Bundle(requireArguments()).apply {
                            if (column == KEY_DATE) {
                                putBoolean(KEY_UPDATE_VALUE_DATE, shouldUpdateValueDate)
                            }
                        }
                        (activity as? ConfirmationDialogListener)?.onPositive(args, shouldClone)
                        dismiss()
                    },
                ) {
                    Text(
                        stringResource(
                            id = if (shouldClone) R.string.button_label_clone_and_remap
                            else R.string.menu_remap,
                        ),
                    )
                }
            }
        }
    }

    companion object {
        const val KEY_TITLE_STRING = "titleString"
        const val KEY_MESSAGE = "message"

        fun newInstance(
            titleString: String,
            message: String,
            column: String,
            value: Long,
        ): RemapDialogFragment {
            return RemapDialogFragment().apply {
                arguments = Bundle().apply {
                    putString(KEY_TITLE_STRING, titleString)
                    putString(KEY_MESSAGE, message)
                    putString(KEY_COLUMN, column)
                    putLong(column, value)
                    putInt(KEY_COMMAND_POSITIVE, R.id.REMAP_COMMAND)
                }
            }
        }
    }
}
