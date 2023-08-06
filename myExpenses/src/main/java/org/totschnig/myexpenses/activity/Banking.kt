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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.viewmodel.BankingViewModel
import org.totschnig.myexpenses.viewmodel.data.Bank

class Banking : ProtectedFragmentActivity() {

    val viewModel: BankingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        setContent {
            val data = viewModel.banks.collectAsState()
            var dialogShown by rememberSaveable { mutableStateOf(false) }
            val tanRequested = viewModel.tanRequested.observeAsState()
            val addBankState = viewModel.addBankState.collectAsState()

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
                        Icon(imageVector = Icons.Filled.Add, contentDescription = "Add new bank")
                    }
                },
            ) {
                Box(
                    modifier = Modifier
                        .padding(it)
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
                                    BankRow(it)
                                }
                            }
                        }
                    }
                }
            }
            if (dialogShown) {
                var bank by rememberSaveable { mutableStateOf(Bank.EMPTY) }
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
                            text = "Add new bank",
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                viewModel.addBank(bank)
                            },
                            enabled = addBankState.value != BankingViewModel.AddBankState.Loading && bank.isComplete
                        ) {
                            Text("Load accounts")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = {
                                dialogShown = false
                                viewModel.resetAddBankState()

                            }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    },
                    text = {
                        Column {
                            OutlinedTextField(
                                value = bank.bankLeitZahl,
                                onValueChange = {
                                    bank = bank.copy(bankLeitZahl = it)
                                },
                                label = { Text(text = "Bankleitzahl") },
                            )
                            OutlinedTextField(
                                value = bank.user,
                                onValueChange = {
                                    bank = bank.copy(user = it)
                                },
                                label = { Text(text = "Anmeldename") },
                            )
                            OutlinedTextField(
                                value = bank.password ?: "",
                                onValueChange = {
                                    bank = bank.copy(password = it)
                                },
                                label = { Text(text = "Passwort") },
                            )

                            if (addBankState.value is BankingViewModel.AddBankState.Error) {
                                Text(
                                    color = MaterialTheme.colorScheme.error,
                                    text = (addBankState.value as BankingViewModel.AddBankState.Error).messsage
                                )
                            } else if (addBankState.value == BankingViewModel.AddBankState.Loading) Text(
                                "Loading ..."
                            )
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

@Composable
fun BankRow(
    bank: org.totschnig.myexpenses.model2.Bank
) {
    Row(
        modifier = Modifier.clickable { },
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
}
