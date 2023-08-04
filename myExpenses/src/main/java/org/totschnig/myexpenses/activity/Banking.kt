package org.totschnig.myexpenses.activity

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CommentBank
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.window.DialogProperties
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.viewmodel.BankingViewModel
import org.totschnig.myexpenses.viewmodel.data.Bank

class Banking : ProtectedFragmentActivity() {

    val viewModel: BankingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContent {
            var dialogShown by rememberSaveable { mutableStateOf(false) }
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
                Box(modifier = Modifier
                    .padding(it)
                    .fillMaxSize()) {

                    val data = viewModel.banks.collectAsState(emptyList()).value
                    if (data.isEmpty()) {
                        Text(
                            text = "No bank added yet.",
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                    LazyColumn(modifier = Modifier.padding(horizontal = dimensionResource(id = R.dimen.padding_main_screen))) {
                        data.forEach {
                            item {
                                Text(it.name)
                            }
                        }
                    }
                }
            }
            if (dialogShown) {
                AlertDialog(
                    properties = DialogProperties(dismissOnClickOutside = false),
                    onDismissRequest = { dialogShown = false },
                    icon = {
                           Icon(imageVector = Icons.Filled.AccountBalance, contentDescription = null)
                    },
                    title = {
                        Text(text = "Add new bank", style = MaterialTheme.typography.titleMedium)
                    },
                    confirmButton = {
                        Button(onClick = { /*TODO*/ }) {
                            Text("Load accounts")
                        }
                    },
                    dismissButton = {
                        Button(onClick = { dialogShown = false }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    },
                    text = {
                        Column {
                            var bank by rememberSaveable { mutableStateOf(Bank.EMPTY) }

                            OutlinedTextField(
                                value = bank.name,
                                onValueChange = {
                                    bank = bank.copy(name = it)
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
                        }
                    }
                )
            }
        }
    }
}