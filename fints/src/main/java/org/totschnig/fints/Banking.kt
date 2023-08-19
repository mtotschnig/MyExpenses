package org.totschnig.fints

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Link
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
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.VisualTransformation
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
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.viewmodel.data.BankingCredentials
import java.time.LocalDate

class Banking : ProtectedFragmentActivity() {

    private val viewModel: BankingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        setContent {
            AppTheme {

                val data = viewModel.banks.collectAsState()
                val dialogShown: MutableState<BankingCredentials?> = rememberSaveable { mutableStateOf(null) }
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
                                dialogShown.value = BankingCredentials.EMPTY
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
                                            },
                                            onShow = {
                                                dialogShown.value = BankingCredentials.fromBank(it)
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                dialogShown.value?.let { bankingCredentials ->
                    val selectedAccounts = rememberMutableStateMapOf<Int, Boolean>()
                    var nrDays: Long? by remember { mutableStateOf(null) }
                    val importMaxDuration = remember { derivedStateOf { nrDays == null } }
                    AlertDialog(
                        //https://issuetracker.google.com/issues/221643630
                        properties = DialogProperties(
                            dismissOnClickOutside = false,
                            usePlatformDefaultWidth = false
                        ),
                        onDismissRequest = {
                            dialogShown.value = null
                            viewModel.reset()
                        },
                        icon = {
                            Icon(
                                imageVector = Icons.Filled.AccountBalance,
                                contentDescription = null
                            )
                        },
                        title = when (workState.value) {
                            is BankingViewModel.WorkState.Done, is BankingViewModel.WorkState.Loading -> null
                            else -> {
                                {
                                    Text(
                                        text = when (workState.value) {
                                            is BankingViewModel.WorkState.AccountsLoaded -> "Select accounts"
                                            BankingViewModel.WorkState.Initial -> if (bankingCredentials.isNew) "Add new bank" else "Enter PIN"
                                            else -> ""
                                        },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
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
                                                }.map { it.first },
                                                nrDays?.let { LocalDate.now().minusDays(it) }
                                            )
                                        }

                                        is BankingViewModel.WorkState.Done -> {
                                            viewModel.reset()
                                            dialogShown.value = null
                                        }

                                        else -> {
                                            viewModel.addBank(bankingCredentials)
                                        }
                                    }
                                },
                                enabled = when (workState.value) {
                                    is BankingViewModel.WorkState.AccountsLoaded -> selectedAccounts.any { it.value }
                                    is BankingViewModel.WorkState.Loading -> false
                                    else -> bankingCredentials.isComplete
                                }
                            ) {
                                Text(
                                    when (workState.value) {
                                        is BankingViewModel.WorkState.AccountsLoaded -> "Import"
                                        is BankingViewModel.WorkState.Done -> "Close"
                                        else -> "Load accounts"
                                    }
                                )
                            }
                        },
                        dismissButton = if (workState.value is BankingViewModel.WorkState.Done) null else {
                            {
                                Button(
                                    onClick = {
                                        dialogShown.value = null
                                        viewModel.reset()

                                    }) {
                                    Text(stringResource(id = android.R.string.cancel))
                                }
                            }
                        },
                        text = {
                            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                                when (workState.value) {
                                    is BankingViewModel.WorkState.Loading -> {
                                        Loading((workState.value as BankingViewModel.WorkState.Loading).message)
                                    }

                                    is BankingViewModel.WorkState.AccountsLoaded -> {
                                        (workState.value as BankingViewModel.WorkState.AccountsLoaded).accounts.forEachIndexed { index, account ->
                                            AccountRow(
                                                account.first,
                                                if (account.second) null else selectedAccounts[index] == true
                                            ) {
                                                selectedAccounts[index] = it
                                            }
                                        }
                                        Column(Modifier.selectableGroup()) {
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .selectable(
                                                        selected = importMaxDuration.value,
                                                        onClick = { nrDays = null },
                                                        role = Role.RadioButton
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                                    selected = importMaxDuration.value,
                                                    onClick = null
                                                )
                                                Text(
                                                    text = "Import maximum available transaction history",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                            Row(
                                                Modifier
                                                    .fillMaxWidth()
                                                    .selectable(
                                                        selected = !importMaxDuration.value,
                                                        onClick = { nrDays = 365 },
                                                        role = Role.RadioButton
                                                    ),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                RadioButton(
                                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                                    selected = !importMaxDuration.value,
                                                    onClick = null
                                                )
                                                Text(
                                                    text = "Import only last ",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                                val interactionSource = remember { MutableInteractionSource() }
                                                BasicTextField(
                                                    value = nrDays?.toString() ?: "",
                                                    onValueChange = { nrDays = try { it.toLong() } catch (e: NumberFormatException) { 0 } },
                                                    interactionSource = interactionSource,
                                                    enabled = !importMaxDuration.value,
                                                    singleLine = true,
                                                    modifier = Modifier
                                                        .width(IntrinsicSize.Min)
                                                        .widthIn(min = 24.dp)
                                                ) {
                                                    OutlinedTextFieldDefaults.DecorationBox(
                                                        value = nrDays?.toString() ?: "",
                                                        innerTextField = it,
                                                        enabled = !importMaxDuration.value,
                                                        singleLine = true,
                                                        visualTransformation = VisualTransformation.None,
                                                        interactionSource = interactionSource
                                                    )
                                                }
                                                Text(
                                                    text = " days",
                                                    style = MaterialTheme.typography.bodyMedium
                                                )
                                            }
                                        }
                                    }

                                    is BankingViewModel.WorkState.Initial -> {
                                        BankingCredentials(
                                            bankingCredentials = dialogShown,
                                            onDone = viewModel::addBank
                                        )
                                    }

                                    is BankingViewModel.WorkState.Done -> {
                                        Text((workState.value as BankingViewModel.WorkState.Done).message)
                                    }
                                    else -> {}
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
                TanDialog(tanRequest = tanRequested.value, submitTan = viewModel::submitTan)
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
    onShow: (Bank) -> Unit
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
            add(MenuEntry(command = "LIST_ACCOUNTS", label = R.string.accounts, icon = Icons.Filled.Checklist) { onShow(bank) })
        }
    )
    HierarchicalMenu(showMenu, menu)
}

@Composable
fun AccountRow(
    account: Konto,
    selected: Boolean?,
    onSelectionChange: (Boolean) -> Unit
) {
    Row {
        if (selected == null) {
            Icon(
                modifier = Modifier.width(48.dp),
                imageVector = Icons.Filled.Link,
                contentDescription = "Account is already imported"
            )
        } else {
            Checkbox(checked = selected, onCheckedChange = onSelectionChange)
        }
        Column {
            Text("${account.type} ${account.name}${account.name2?.let { " $it" } ?: ""}")
            Text(account.dbNumber)
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
