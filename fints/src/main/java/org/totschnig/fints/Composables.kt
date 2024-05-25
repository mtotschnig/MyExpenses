package org.totschnig.fints

import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.job
import org.totschnig.myexpenses.compose.optional
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.R as RB

@Composable
fun ColumnScope.BankingCredentials(
    bankingCredentials: MutableState<BankingCredentials>,
    onDone: (BankingCredentials) -> Unit
) {
    val credentials = bankingCredentials.value
    credentials.bank?.let { Text(it.bankName) } ?: run {
        OutlinedTextField(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = credentials.isNew,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next
            ),
            value = credentials.bankLeitZahl,
            onValueChange = {
                bankingCredentials.value = credentials.copy(bankLeitZahl = it.trim())
            },
            label = { Text(text = stringResource(id = R.string.bankleitzahl)) },
            singleLine = true
        )
    }
    OutlinedTextField(
        modifier = Modifier.align(Alignment.CenterHorizontally),
        enabled = credentials.isNew,
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        value = credentials.user,
        onValueChange = {
            bankingCredentials.value = credentials.copy(user = it.trim())
        },
        label = { Text(text = stringResource(id = R.string.login_name)) },
        singleLine = true
    )
    val focusRequester =
        if (credentials.isNew) null else remember { FocusRequester() }.also { requester ->
            LaunchedEffect(Unit) {
                this.coroutineContext.job.invokeOnCompletion {
                    requester.requestFocus()
                }
            }
        }
    OutlinedTextField(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .optional(focusRequester) { this.focusRequester(it) },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done
        ),
        keyboardActions = if (credentials.isComplete) KeyboardActions(
            onDone = {
                onDone(credentials)
            }
        ) else KeyboardActions.Default,
        value = credentials.password ?: "",
        onValueChange = {
            bankingCredentials.value = credentials.copy(password = it.trim())
        },
        label = { Text(text = stringResource(id = RB.string.password)) },
        singleLine = true
    )
    // Not using supportingText parameter of OutlinedTextField, because of
    // https://issuetracker.google.com/issues/270523016
    Text(
        modifier = Modifier.width(OutlinedTextFieldDefaults.MinWidth),
        text = stringResource(id = R.string.pin_info)
    )
}

@Composable
fun PushTanDialog(
    msg: String?,
    confirmPushTan: () -> Unit
) {
    msg?.let {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(onClick = {
                    confirmPushTan()
                }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            text = {
                Column {
                    Text(msg)
                    Text(stringResource(id = R.string.pushtan_dialog))
                }
            }

        )
    }
}

@Composable
fun TanDialog(
    tanRequest: TanRequest?,
    submitTan: (String?) -> Unit
) {
    tanRequest?.let {
        var tan by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                submitTan(null)
            },
            confirmButton = {
                Button(onClick = {
                    submitTan(tan)
                }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            text = {
                Column {
                    Text(tanRequest.message)
                    tanRequest.bitmap?.let {
                        Image(bitmap = it.asImageBitmap(), contentDescription = null)
                    }
                    OutlinedTextField(
                        value = tan,
                        onValueChange = {
                            tan = it
                        },
                        label = { Text(text = "TAN") },
                    )
                }
            }
        )
    }
}

@Composable
fun SecMechDialog(
    options: List<SecMech>?,
    submitSecMech: (Pair<String, Boolean>?) -> Unit
) {
    options?.let {
        SelectionDialog(
            title = R.string.sec_mech_selection_prompt,
            options = options.map { it.id to it.name },
            submit = submitSecMech
        )
    }
}

@Composable
fun TanMediaDialog(
    options: List<String>?,
    submitMedia: (Pair<String, Boolean>?) -> Unit
) {
    options?.let {
        SelectionDialog(
            title = R.string.tan_medium_selection_prompt,
            options = options.map { it to it },
            submit = submitMedia
        )
    }
}

@Composable
private fun SelectionDialog(
    @StringRes title: Int,
    options: List<Pair<String, String>>,
    submit: (Pair<String, Boolean>?) -> Unit
) {
    val (selectedOption, onOptionSelected) = rememberSaveable { mutableStateOf(options[0]) }
    val (shouldSaveSelection, onShouldSaveSelectionChanged) = rememberSaveable { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = {
            submit(null)
        },
        confirmButton = {
            Button(onClick = {
                submit(selectedOption.first to shouldSaveSelection)
            }) {
                Text(stringResource(id = android.R.string.ok))
            }
        },
        text = {
            Column(Modifier.selectableGroup()) {
                Text(stringResource(title))
                options.forEach { option ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = option == selectedOption,
                                onClick = { onOptionSelected(option) },
                                role = Role.RadioButton
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            modifier = Modifier.minimumInteractiveComponentSize(),
                            selected = option == selectedOption,
                            onClick = null
                        )
                        Text(
                            text = option.second,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = shouldSaveSelection,
                        onCheckedChange = onShouldSaveSelectionChanged
                    )
                    Text(text = stringResource(id = R.string.checkbox_should_save_selection_label))
                }
            }
        }
    )
}

@Composable
fun BankIconImpl(modifier: Modifier = Modifier, bank: Bank) {
    bank.asWellKnown?.icon?.let {
        Image(modifier = modifier, painter = painterResource(id = it), contentDescription = null)
    } ?: run {
        Image(
            modifier = modifier,
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = null
        )
    }
}

@Preview
@Composable
fun Loading(text: String? = "Loading") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        CircularProgressIndicator()
        Text(text ?: stringResource(id = org.totschnig.myexpenses.R.string.loading))
    }
}

@Composable
fun Error(errorMessage: String?) {
    errorMessage?.let {
        Text(
            color = MaterialTheme.colorScheme.error,
            text = it
        )
    }
}

@Preview
@Composable
fun TanMediaPreview() {
    TanMediaDialog(options = listOf("pushTan", "Pixel"), submitMedia = {})
}
