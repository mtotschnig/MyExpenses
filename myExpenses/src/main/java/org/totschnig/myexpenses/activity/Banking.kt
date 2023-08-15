package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.kapott.hbci.structures.Konto
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.rememberMutableStateMapOf
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model2.Bank
import org.totschnig.myexpenses.viewmodel.BankingViewModel
import org.totschnig.myexpenses.viewmodel.data.BankingCredentials

class Banking : ProtectedFragmentActivity() {

    val viewModel: BankingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        viewModel.initAttributes()
        setContent {
            AppTheme {

                val data = viewModel.banks.collectAsState()
                var dialogShown by rememberSaveable { mutableStateOf(false) }
                val tanRequested = viewModel.tanRequested.observeAsState()
                val workState = viewModel.workState.collectAsState()
                val errorState = viewModel.errorState.collectAsState()

                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Banking") },
                            navigationIcon = {
                                IconButton(onClick = { finish() }) {
                                    Icon(
                                        imageVector = Icons.Filled.ArrowBack,
                                        contentDescription = stringResource(id = androidx.appcompat.R.string.abc_action_bar_up_description)
                                    )
                                }
                            }
                        )
                    },
                    floatingActionButtonPosition = FabPosition.End,
                    floatingActionButton = {
                        FloatingActionButton(
                            onClick = {
                                dialogShown = true
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = "Add new bank"
                            )
                        }
                    },
                ) { paddingValues ->
                    Box(
                        modifier = Modifier
                            .padding(paddingValues)
                            .fillMaxSize()
                    ) {
                        if (data.value.isEmpty()) {
                            Text(
                                text = "No bank added yet.",
                                modifier = Modifier.align(Alignment.Center)
                            )
                        } else {
                            LazyColumn(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_main_screen))) {
                                data.value.forEach {
                                    item {
                                        BankRow(
                                            bank = it,
                                            onDelete = {
                                                if (it.count > 0) {
                                                    confirmBankDelete(it)
                                                } else {
                                                    viewModel.deleteBank(it.id)
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                if (dialogShown) {
                    var bankingCredentials by rememberSaveable { mutableStateOf(BankingCredentials.EMPTY) }
                    val selectedAccounts = rememberMutableStateMapOf<Int, Boolean>()
                    AlertDialog(
                        //https://issuetracker.google.com/issues/221643630
                        properties = DialogProperties(
                            dismissOnClickOutside = false,
                            usePlatformDefaultWidth = false
                        ),
                        onDismissRequest = {
                            dialogShown = false
                            viewModel.resetAddBankState()
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.AccountBalance,
                                contentDescription = null
                            )
                        },
                        title = {
                            Text(
                                text = if (workState.value is BankingViewModel.WorkState.AccountsLoaded) "Select accounts" else "Add new bank",
                                style = MaterialTheme.typography.titleMedium
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    when (workState.value) {
                                        is BankingViewModel.WorkState.AccountsLoaded -> {
                                            val state =
                                                workState.value as BankingViewModel.WorkState.AccountsLoaded
                                            viewModel.importAccounts(
                                                bankingCredentials,
                                                state.bank,
                                                state.accounts.filterIndexed { index, _ ->
                                                    selectedAccounts[index] == true
                                                }
                                            )
                                        }
                                        is BankingViewModel.WorkState.Done -> {
                                            viewModel.reset()
                                            dialogShown = false
                                        }
                                        else -> {
                                            viewModel.addBank(bankingCredentials)
                                        }
                                    }
                                },
                                enabled = when (workState.value) {
                                    is BankingViewModel.WorkState.AccountsLoaded -> selectedAccounts.size > 0
                                    is BankingViewModel.WorkState.Loading -> false
                                    else -> bankingCredentials.isComplete
                                }
                            ) {
                                Text(
                                    when (workState.value) {
                                        is BankingViewModel.WorkState.AccountsLoaded -> "Import"
                                        is BankingViewModel.WorkState.Done  -> "Close"
                                        else -> "Load accounts"
                                    }
                                )
                            }
                        },
                        dismissButton = if (workState.value is BankingViewModel.WorkState.Done) null else {
                            {
                                Button(
                                    onClick = {
                                        dialogShown = false
                                        viewModel.resetAddBankState()

                                    }) {
                                    Text(stringResource(id = android.R.string.cancel))
                                }
                            }
                        },
                        text = {
                            Column {
                                when (workState.value) {
                                    is BankingViewModel.WorkState.Loading -> {
                                        Loading((workState.value as BankingViewModel.WorkState.Loading).message)
                                    }

                                    is BankingViewModel.WorkState.AccountsLoaded -> {
                                        (workState.value as BankingViewModel.WorkState.AccountsLoaded).accounts.forEachIndexed { index, account ->
                                            AccountRow(account, selectedAccounts[index] == true) {
                                                selectedAccounts[index] = it
                                            }
                                        }
                                    }

                                    is BankingViewModel.WorkState.Initial -> {
                                        OutlinedTextField(
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            value = bankingCredentials.bankLeitZahl,
                                            onValueChange = {
                                                bankingCredentials =
                                                    bankingCredentials.copy(bankLeitZahl = it)
                                            },
                                            label = { Text(text = "Bankleitzahl") },
                                        )
                                        OutlinedTextField(
                                            value = bankingCredentials.user,
                                            onValueChange = {
                                                bankingCredentials =
                                                    bankingCredentials.copy(user = it)
                                            },
                                            label = { Text(text = "Anmeldename") },
                                        )
                                        OutlinedTextField(
                                            visualTransformation = PasswordVisualTransformation(),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                                            value = bankingCredentials.password ?: "",
                                            onValueChange = {
                                                bankingCredentials =
                                                    bankingCredentials.copy(password = it)
                                            },
                                            label = { Text(text = "PIN") },
                                            supportingText = { Text(text = "Der PIN wird nur beim erstmaligen Laden der Daten verwendet, und nicht auf dem Gerät gespeichert. Bei jeder weiteren Verbindung mit der Bank wird er erneut abgefragt.") }
                                        )
                                    }

                                    is BankingViewModel.WorkState.Done -> {
                                        Text((workState.value as BankingViewModel.WorkState.Done).message)
                                    }
                                }
                                errorState.value?.let {
                                    Text(
                                        color = MaterialTheme.colorScheme.error,
                                        text = it
                                    )
                                }
                            }
                        }
                    )
                }
                if (tanRequested.value == true) {
                    var tan by rememberSaveable { mutableStateOf("") }
                    AlertDialog(
                        onDismissRequest = {
                            viewModel.submitTan(null)
                        },
                        confirmButton = {
                            Button(onClick = {
                                viewModel.submitTan(tan)
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
            }
        }
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        if (super.dispatchCommand(command, tag)) true else when (command) {
            R.id.DELETE_BANK_COMMAND_DO -> {
                viewModel.deleteBank(tag as Long)
                true
            }

            else -> false
        }

    private fun confirmBankDelete(bank: Bank) {
        MessageDialogFragment.newInstance(
            "Delete bank?",
            "${bank.count} accounts are linked to $bank. By deleting it, you will no longer be able to download data for them." + " " + getString(
                R.string.continue_confirmation
            ),
            MessageDialogFragment.Button(
                R.string.menu_delete,
                R.id.DELETE_BANK_COMMAND_DO,
                bank.id
            ),
            null,
            MessageDialogFragment.noButton(), 0
        )
            .show(supportFragmentManager, "DELETE_ACCOUNT")
    }
}

@Composable
fun BankRow(
    bank: Bank,
    onDelete: (Bank) -> Unit,
) {
    val showMenu = remember { mutableStateOf(false) }
    Row(
        modifier = Modifier.clickable { showMenu.value = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Image(imageVector = Icons.Filled.AccountBalance, contentDescription = null)
        Column {
            Text(bank.bankName)
            Text(bank.blz + " / " + bank.bic)
            Text(bank.userId)
        }
    }
    val menu = Menu(
        buildList {
            add(MenuEntry.delete("DELETE_BANK") { onDelete(bank) })
        }
    )
    HierarchicalMenu(showMenu, menu)
}

@Composable
fun AccountRow(
    account: Konto,
    selected: Boolean,
    onSelectionChange: (Boolean) -> Unit
) {
    Row {
        Checkbox(checked = selected, onCheckedChange = onSelectionChange)
        Column {
            Text(account.name + (account.name2?.let { " $it" } ?: ""))
            Text(account.iban)
        }
    }
}

@Preview
@Composable
fun Loading(text: String = "Loading") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CircularProgressIndicator()
        Text("$text ...")
    }
}
