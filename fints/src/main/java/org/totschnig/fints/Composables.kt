package org.totschnig.fints

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation

@Composable
fun BankingCredentials(
    bankingCredentials: MutableState<BankingCredentials?>,
    onDone: (BankingCredentials) -> Unit
) {
    bankingCredentials.value?.let { credentials ->
        credentials.bank?.let { Text(it.bankName) } ?: run {
            OutlinedTextField(
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
            enabled = credentials.isNew,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
            value = credentials.user,
            onValueChange = {
                bankingCredentials.value = credentials.copy(user = it.trim())
            },
            label = { Text(text = stringResource(id = R.string.login_name)) },
            singleLine = true
        )
        OutlinedTextField(
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
            label = { Text(text = "PIN") },
            singleLine = true
        )
        // Not using supportingText parameter of OutlinedTextField, because of
        // https://issuetracker.google.com/issues/270523016
        Text(text = stringResource(id = R.string.pin_info))
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
                    Text("Send")
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