package org.totschnig.myexpenses.designsystem

import android.text.format.DateFormat
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Asks for a text, e.g. the value of a setting
 *
 * @param placeholder shown while the field is empty
 * @param allowBlank whether an empty text can be confirmed, e.g. to go back to a default
 * @param keyboardType e.g. [KeyboardType.Number] for a number
 * @param isPassword hides what is typed
 * @param supportingText below the field, e.g. what the text is used for
 * @param intro above the field, e.g. what the letters of a pattern stand for
 * @param preview below the field instead of [supportingText] while typing, e.g. a date in the
 * pattern typed
 * @param error of the text typed, shown below the field instead of [preview], null if it is fine;
 * the text cannot be confirmed while there is one
 */
@Composable
fun TextDialog(
    title: String,
    initialText: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    placeholder: String? = null,
    allowBlank: Boolean = true,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false,
    supportingText: String? = null,
    intro: String? = null,
    preview: ((String) -> String?)? = null,
    error: ((String) -> String?)? = null,
) {
    var text by rememberSaveable { mutableStateOf(initialText) }
    val errorText = error?.invoke(text)
    val below = errorText ?: preview?.invoke(text) ?: supportingText
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                intro?.let { DialogIntro(it) }
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    placeholder = placeholder?.let { { Text(it) } },
                    supportingText = below?.let { { Text(it) } },
                    // The title says what the only field is for, so it is not repeated as a label
                    modifier = Modifier.semantics { contentDescription = title },
                    isError = errorText != null,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = if (isPassword) KeyboardType.Password else keyboardType
                    ),
                    visualTransformation = if (isPassword) PasswordVisualTransformation() else VisualTransformation.None
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = (allowBlank || text.isNotBlank()) && errorText == null) {
                Text(stringResource(R.string.ds_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Asks for a time of day, e.g. when the daily backup runs
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimeDialog(
    title: String,
    hour: Int,
    minute: Int,
    onConfirm: (hour: Int, minute: Int) -> Unit,
    onDismiss: () -> Unit,
) {
    val state = rememberTimePickerState(hour, minute, DateFormat.is24HourFormat(LocalContext.current))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { TimePicker(state) },
        confirmButton = {
            TextButton(onClick = { onConfirm(state.hour, state.minute) }) {
                Text(stringResource(R.string.ds_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Asks for a day, e.g. until when money is saved for a goal
 *
 * @param initial preselected, null for today
 * @param onClear removes the day, e.g. for a goal without date; no button if null
 * @param clearLabel of the button calling [onClear], shown next to the title, so that the dialog
 * keeps two actions at the bottom
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateDialog(
    initial: LocalDate?,
    onConfirm: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onClear: (() -> Unit)? = null,
    clearLabel: String? = null,
) {
    val state = rememberDatePickerState(
        initialSelectedDateMillis = (initial ?: LocalDate.now()).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    state.selectedDateMillis?.let {
                        onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                    }
                },
                enabled = state.selectedDateMillis != null
            ) {
                Text(stringResource(R.string.ds_apply))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    ) {
        DatePicker(
            state,
            title = {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 12.dp, top = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DatePickerDefaults.DatePickerTitle(state.displayMode, Modifier.weight(1f))
                    if (onClear != null && clearLabel != null) {
                        TextButton(onClick = onClear) { Text(clearLabel) }
                    }
                }
            }
        )
    }
}

/**
 * Lets the user pick one of [options]; picking one closes the dialog
 *
 * @param intro shown above the options
 * @param trailing at the end of an option, e.g. a button to edit it
 * @param footer below the options, e.g. a [DialogActionRow] to add a new option
 */
@Composable
fun <T> SingleChoiceDialog(
    title: String,
    options: List<T>,
    selected: T?,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    intro: String? = null,
    trailing: (@Composable (T) -> Unit)? = null,
    footer: (@Composable () -> Unit)? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                Modifier
                    .selectableGroup()
                    .verticalScroll(rememberScrollState())
            ) {
                intro?.let { DialogIntro(it) }
                options.forEach { option ->
                    val isSelected = option == selected
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(selected = isSelected, role = Role.RadioButton, onClick = { onSelect(option) })
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isSelected, onClick = null)
                        Text(
                            label(option),
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        )
                        trailing?.invoke(option)
                    }
                }
                footer?.invoke()
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Asks to confirm an action that cannot easily be undone
 */
@Composable
fun ConfirmDialog(
    title: String,
    text: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * Lets the user pick one of [options] to act on, e.g. a contract to merge with; picking one closes
 * the dialog. Unlike [SingleChoiceDialog] nothing is selected before.
 *
 * @param supportingText second line of an option
 * @param value on the right of an option, e.g. an amount
 * @param emptyText shown when there are no options
 */
@Composable
fun <T> PickDialog(
    title: String,
    options: List<T>,
    label: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    onDismiss: () -> Unit,
    intro: String? = null,
    supportingText: @Composable (T) -> String? = { null },
    value: @Composable (T) -> String? = { null },
    emptyText: String? = null,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                intro?.let { DialogIntro(it) }
                if (options.isEmpty()) emptyText?.let { Text(it, style = MaterialTheme.typography.bodyLarge) }
                options.forEach { option ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option) }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                label(option),
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            supportingText(option)?.let {
                                Text(
                                    it,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        value(option)?.let {
                            Text(it, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(android.R.string.cancel))
            }
        }
    )
}

/**
 * An action below the options or fields of a dialog, e.g. to add a new option
 */
@Composable
fun DialogActionRow(icon: ImageVector, text: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Text(
            text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp)
        )
    }
}

/**
 * A button in a dialog for something that removes or deletes, in the error color
 */
@Composable
fun DestructiveTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
) {
    TextButton(
        onClick = onClick,
        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
        modifier = modifier
    ) {
        icon?.let { Icon(it, contentDescription = null, modifier = Modifier.padding(end = 8.dp)) }
        Text(text)
    }
}

/** What a dialog is about, above its options or fields */
@Composable
internal fun DialogIntro(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(bottom = 8.dp)
    )
}

