package org.totschnig.fints

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
    HbciVersionSelection(bankingCredentials)
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
    val focusRequester = if (credentials.isNew) null else remember { FocusRequester() }.also { requester ->
        LaunchedEffect(Unit) {
            this.coroutineContext.job.invokeOnCompletion {
                requester.requestFocus()
            }
        }
    }
    OutlinedTextField(
        modifier = Modifier
            .align(Alignment.CenterHorizontally)
            .optional(focusRequester, ifPresent = { this.focusRequester(it) }),
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
        label = { Text(text = stringResource(id = RB.string.hint_password)) },
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

@Composable
fun BankIconImpl(modifier: Modifier = Modifier, bank: Bank) {
    getIcon(bank)?.let {
        Image(modifier = modifier, painter = painterResource(id = it), contentDescription = null)
    } ?: run {
        Image(
            modifier = modifier,
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = null
        )
    }
}

private fun getIcon(bank: Bank) = when {
    bank.blz == "12030000" -> R.drawable.dkb
    bank.blz == "43060967" -> R.drawable.gls
    bank.blz == "50010517" -> R.drawable.ing
    bank.blz.startsWith("200411") -> R.drawable.comdirect
    bank.blz[3] == '5' -> R.drawable.sparkasse
    bank.blz[3] == '9' -> R.drawable.volksbank
    bank.bankName.contains("sparda", ignoreCase = true) -> R.drawable.sparda
    else -> null
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HbciVersionSelection(
    credentials : MutableState<BankingCredentials>
) {

    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier.menuAnchor(),
            readOnly = true,
            value = credentials.value.hbciVersion.getName(),
            onValueChange = {},
            label = { Text("HBCI-Version") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SUPPORTED_HBCI_VERSIONS.forEach { selectionOption ->
                DropdownMenuItem(
                    text = { Text(selectionOption.getName()) },
                    onClick = {
                        credentials.value = credentials.value.copy(hbciVersion = selectionOption)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                )
            }
        }
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
