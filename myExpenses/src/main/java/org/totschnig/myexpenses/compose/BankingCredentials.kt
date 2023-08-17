package org.totschnig.myexpenses.compose

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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import org.totschnig.myexpenses.viewmodel.data.BankingCredentials

@Composable
fun BankingCredentials(
    bankingCredentials: MutableState<BankingCredentials?>,
    onDone: (BankingCredentials) -> Unit
) {
    bankingCredentials.value?.let { credentials ->
        credentials.bank?.let {
            Text(it.second)
        } ?: run {
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
                label = { Text(text = "Bankleitzahl") },
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
            label = { Text(text = "Anmeldename") },
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
            singleLine = true,
            supportingText = { Text(text = "Der PIN wird nicht auf dem Gerät gespeichert und bei jeder weiteren Verbindung mit der Bank erneut abgefragt.") }
        )
    }
}

@Composable
fun TanDialog(submitTan: (String?) -> Unit) {
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
            OutlinedTextField(
                value = tan,
                onValueChange = {
                    tan = it
                },
                label = { Text(text = "TAN") },
            )
        }
    )
}