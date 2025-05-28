package org.totschnig.fints

import android.graphics.Bitmap
import android.graphics.Color
import android.os.Parcelable
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.compose.optional
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.util.safeMessage
import kotlin.random.Random
import org.totschnig.myexpenses.R as RB
import androidx.core.graphics.createBitmap
import androidx.core.graphics.set

@Composable
fun ColumnScope.BankingCredentials(
    bankingCredentials: MutableState<BankingCredentials>,
    onDone: (BankingCredentials) -> Unit,
) {
    val credentials = bankingCredentials.value
    credentials.bank?.let { Text(it.bankName) } ?: run {
        OutlinedTextField(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            enabled = credentials.isNew,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Next,
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
        modifier = Modifier.align(Alignment.CenterHorizontally)
            .semantics { this.contentType = ContentType.Username },
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
    var showPassword by remember { mutableStateOf(false) }
    OutlinedTextField(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .semantics { this.contentType = ContentType.Password }
            .optional(focusRequester) { this.focusRequester(it) },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
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
        singleLine = true,
        supportingText = {
            Text(text = stringResource(id = R.string.pin_info))
        },
        trailingIcon = {
            PasswordVisibilityToggleIcon(
                showPassword = showPassword,
                onTogglePasswordVisibility = { showPassword = !showPassword })
        }
    )
}

@Composable
fun PasswordVisibilityToggleIcon(
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
) {
    val image = if (showPassword) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
    val contentDescription = stringResource(if (showPassword) RB.string.hide_password else RB.string.show_password)

    IconButton(onClick = onTogglePasswordVisibility) {
        Icon(imageVector = image, contentDescription = contentDescription)
    }
}

@Composable
fun PushTanDialog(pushTanRequest: PushTanRequest?) {
    pushTanRequest?.let {
        AlertDialog(
            onDismissRequest = { },
            confirmButton = {
                Button(onClick = pushTanRequest.submit) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            text = {
                Column {
                    Text(pushTanRequest.message)
                    Text(stringResource(id = R.string.pushtan_dialog))
                }
            }
        )
    }
}

@Composable
fun TanDialog(tanRequest: TanRequest?) {
    tanRequest?.let {
        var tan by rememberSaveable { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = {
                tanRequest.submit(null)
            },
            confirmButton = {
                Button(onClick = {
                    tanRequest.submit(tan)
                }) {
                    Text(stringResource(id = android.R.string.ok))
                }
            },
            text = {
                Column {
                    Text(tanRequest.message)
                    tanRequest.bitmap?.let {
                        Image(
                            modifier = Modifier
                                .height(200.dp)
                                .width(200.dp),
                            bitmap = it.asImageBitmap(),
                            contentDescription = null
                        )
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
fun SecMechDialog(request: SecMechRequest?) {
    request?.let {
        SelectionDialog(
            title = R.string.sec_mech_selection_prompt,
            options = request.options.map { it.id to it.name },
            submit = request.submit
        )
    }
}

@Composable
fun TanMediaDialog(request: TanMediumRequest?) {
    request?.let {
        SelectionDialog(
            title = R.string.tan_medium_selection_prompt,
            options = request.options.map { it to it },
            submit = request.submit
        )
    }
}

@Composable
private fun SelectionDialog(
    @StringRes title: Int,
    options: List<Pair<String, String>>,
    submit: (Pair<String, Boolean>?) -> Unit,
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

sealed class MigrationState : Parcelable {
    @Parcelize
    data object Idle : MigrationState()

    @Parcelize
    data object Running : MigrationState()

    @Parcelize
    data object Success : MigrationState()

    @Parcelize
    data class Failure(val message: String) : MigrationState()
}

@Composable
fun MigrationDialog(
    migrationDialogShown: MutableState<Bank?>,
    onMigrate: suspend (Bank, String) -> Result<Unit>,
) {
    val scope = rememberCoroutineScope()
    var passphrase by rememberSaveable { mutableStateOf("") }
    var migrationState by rememberSaveable { mutableStateOf<MigrationState>(MigrationState.Idle) }
    migrationDialogShown.value?.let { bank ->
        fun doDismiss() {
            migrationDialogShown.value = null
            migrationState = MigrationState.Idle
        }
        AlertDialog(
            onDismissRequest = {
                if (migrationState != MigrationState.Running) {
                    doDismiss()
                }
            },
            confirmButton = {
                when (migrationState) {
                    MigrationState.Running -> {
                        CircularProgressIndicator()
                    }

                    MigrationState.Idle, is MigrationState.Failure -> {
                        Button(
                            onClick = {
                                scope.launch {
                                    migrationState = MigrationState.Running
                                    onMigrate(bank, passphrase).onSuccess {
                                        migrationState = MigrationState.Success
                                    }.onFailure {
                                        migrationState = MigrationState.Failure(it.safeMessage)
                                        passphrase = ""
                                    }
                                }
                            },
                            enabled = passphrase.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.migrate))
                        }
                    }

                    else -> {}
                }
            },
            dismissButton = if (migrationState == MigrationState.Running)
                null
            else {
                {

                    Button(onClick = ::doDismiss) {
                        Text(stringResource(id = if (migrationState == MigrationState.Success) android.R.string.ok else android.R.string.cancel))
                    }
                }
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(stringResource(R.string.migration_description_1548))

                    Error((migrationState as? MigrationState.Failure)?.message)
                    if (migrationState == MigrationState.Success) {
                        Text(stringResource(R.string.password_securely_stored))
                    } else {
                        OutlinedTextField(
                            enabled = migrationState != MigrationState.Running,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally),
                            visualTransformation = PasswordVisualTransformation(),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Password,
                                imeAction = ImeAction.Done
                            ),
                            keyboardActions = KeyboardActions(
                                onDone = {}
                            ),
                            value = passphrase,
                            onValueChange = {
                                passphrase = it.trim()
                            },
                            label = { Text(text = stringResource(id = RB.string.password)) },
                            singleLine = true
                        )
                    }
                }
            }
        )
    }
}

@Composable
fun BankIconImpl(modifier: Modifier = Modifier, bank: Bank) {
    bank.asWellKnown?.icon?.let {
        Image(modifier = modifier, painter = painterResource(id = it), contentDescription = null)
    } ?: run {
        Icon(
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
        Text(text ?: stringResource(id = RB.string.loading))
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
private fun TanMediaPreview() {
    TanMediaDialog(TanMediumRequest(options = listOf("pushTan", "Pixel")) {})
}

@Preview
@Composable
private fun TanPreview() {
    val bmp = createRandomBitmap(50, 50)
    TanDialog(TanRequest("Please scan", bmp) {})
}

private fun createRandomBitmap(width: Int, height: Int): Bitmap {
    val bitmap = createBitmap(width, height)
    for (x in 0 until width) {
        for (y in 0 until height) {
            val color = Color.rgb(
                Random.nextInt(256),
                Random.nextInt(256),
                Random.nextInt(256)
            )
            bitmap[x, y] = color
        }
    }
    return bitmap
}
