package org.totschnig.fints

import android.os.Bundle
import android.text.TextUtils
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FabPosition
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import org.kapott.hbci.structures.Konto
import org.totschnig.fints.BankingViewModel.WorkState.*
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.DenseTextField
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.rememberMutableStateListOf
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.injector
import org.totschnig.myexpenses.model2.Bank
import java.time.LocalDate
import org.totschnig.fints.R as RF

class Banking : ProtectedFragmentActivity() {

    private val viewModel: BankingViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        injector.inject(viewModel)
        setContent {
            AppTheme {

                val data = viewModel.banks.collectAsState()
                val dialogShown: MutableState<BankingCredentials?> =
                    rememberSaveable { mutableStateOf(null) }
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
                                contentDescription = stringResource(id = RF.string.add_new_bank)
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
                                text = stringResource(org.totschnig.fints.R.string.no_bank_added_yet),
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
                    val selectedAccounts = rememberMutableStateListOf<Int>()
                    var nrDays: Long? by remember { mutableStateOf(null) }
                    val importMaxDuration = remember { derivedStateOf { nrDays == null } }

                    val availableAccounts = viewModel.accounts.collectAsState(initial = emptyList())
                    val targetOptions = remember {
                        derivedStateOf {
                            buildList {
                                add(0L to getString(R.string.menu_create_account))
                                availableAccounts.value.forEach {
                                    add(it.id to it.label)
                                }
                            }
                        }
                    }
                    val targetSelectionEnabled = remember {
                        derivedStateOf { selectedAccounts.size < 2 }
                    }
                    var selectedTargetOption by rememberSaveable(targetSelectionEnabled.value) {
                        mutableStateOf(targetOptions.value[0])
                    }

                    AlertDialog(
                        properties = DialogProperties(
                            dismissOnClickOutside = false
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
                            is AccountsLoaded -> RF.string.select_accounts
                            Initial -> if (bankingCredentials.isNew) RF.string.add_new_bank else RF.string.enter_pin
                            else -> null
                        }?.let {
                            {
                                Text(
                                    text = stringResource(id = it),
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    when (workState.value) {
                                        is AccountsLoaded -> {
                                            val state =
                                                workState.value as AccountsLoaded
                                            viewModel.importAccounts(
                                                bankingCredentials,
                                                state.bank,
                                                state.accounts.filterIndexed { index, _ ->
                                                    selectedAccounts.contains(index)
                                                }.map { it.first },
                                                nrDays?.let { LocalDate.now().minusDays(it) },
                                                selectedTargetOption.first
                                            )
                                        }

                                        is Done -> {
                                            viewModel.reset()
                                            dialogShown.value = null
                                        }

                                        else -> {
                                            viewModel.addBank(bankingCredentials)
                                        }
                                    }
                                },
                                enabled = when (workState.value) {
                                    is AccountsLoaded -> selectedAccounts.size > 0
                                    is Loading -> false
                                    else -> bankingCredentials.isComplete
                                }
                            ) {
                                Text(
                                    stringResource(
                                        when (workState.value) {
                                            is AccountsLoaded -> R.string.menu_import
                                            is Done -> R.string.menu_close
                                            else -> RF.string.btn_load_accounts
                                        }
                                    )
                                )
                            }
                        },
                        dismissButton = if (workState.value is Done) null else {
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
                            Column(modifier = Modifier.width(IntrinsicSize.Min).verticalScroll(rememberScrollState())) {
                                when (workState.value) {
                                    is Loading -> {
                                        Loading((workState.value as Loading).message)
                                    }

                                    is AccountsLoaded -> {
                                        val accounts =
                                            (workState.value as AccountsLoaded).accounts
                                        accounts.forEachIndexed { index, account ->
                                            AccountRow(
                                                account.first,
                                                if (account.second) null else selectedAccounts.contains(
                                                    index
                                                )
                                            ) {
                                                if (it) selectedAccounts.add(index) else selectedAccounts.remove(
                                                    index
                                                )
                                            }
                                        }
                                        if (!accounts.all { it.second }) {
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
                                                        text = getString(org.totschnig.fints.R.string.import_maximum),
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                                Row(
                                                    Modifier
                                                        .fillMaxWidth()
                                                        .selectable(
                                                            selected = !importMaxDuration.value,
                                                            onClick = { if (nrDays == null) nrDays = 365 },
                                                            role = Role.RadioButton
                                                        ),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    RadioButton(
                                                        modifier = Modifier.minimumInteractiveComponentSize(),
                                                        selected = !importMaxDuration.value,
                                                        onClick = null
                                                    )
                                                    val parts =
                                                        stringResource(id = RF.string.import_only_n).split(
                                                            '|'
                                                        )
                                                    Text(
                                                        text = parts[0],
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                    DenseTextField(
                                                        value = nrDays?.toString() ?: "",
                                                        onValueChange = {
                                                            nrDays = try {
                                                                it.toLong()
                                                            } catch (e: NumberFormatException) {
                                                                0
                                                            }
                                                        },
                                                        modifier = Modifier
                                                            .width(IntrinsicSize.Min)
                                                            .widthIn(min = 24.dp),
                                                        keyboardOptions = KeyboardOptions(
                                                            keyboardType = KeyboardType.Number
                                                        )
                                                    )
                                                    Text(
                                                        text = parts[1],
                                                        style = MaterialTheme.typography.bodyMedium
                                                    )
                                                }
                                            }

                                            var expanded by rememberSaveable { mutableStateOf(false) }

                                            ExposedDropdownMenuBox(
                                                expanded = expanded,
                                                onExpandedChange = {
                                                    if (targetSelectionEnabled.value) expanded = !expanded
                                                },
                                            ) {
                                                TextField(
                                                    enabled = targetSelectionEnabled.value,
                                                    modifier = Modifier.menuAnchor(),
                                                    readOnly = true,
                                                    value = selectedTargetOption.second,
                                                    onValueChange = {},
                                                    label = { Text(stringResource(id = R.string.account)) },
                                                    trailingIcon = {
                                                        if (targetSelectionEnabled.value) ExposedDropdownMenuDefaults.TrailingIcon(
                                                            expanded = expanded
                                                        )
                                                    },
                                                    colors = ExposedDropdownMenuDefaults.textFieldColors(),
                                                )
                                                ExposedDropdownMenu(
                                                    expanded = expanded,
                                                    onDismissRequest = { expanded = false },
                                                ) {
                                                    targetOptions.value.forEach {
                                                        DropdownMenuItem(
                                                            text = { Text(it.second) },
                                                            onClick = {
                                                                selectedTargetOption = it
                                                                expanded = false
                                                            },
                                                            contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding,
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    is Initial -> {
                                        BankingCredentials(
                                            bankingCredentials = dialogShown,
                                            onDone = viewModel::addBank
                                        )
                                    }

                                    is Done -> {
                                        Text((workState.value as Done).message)
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
            getString(RF.string.dialog_title_delete_bank),
            TextUtils.concat(
                resources.getQuantityString(
                    RF.plurals.warning_delete_bank_1,
                    bank.count,
                    bank.count,
                    bank
                ),
                " ",
                getString(RF.string.wwrning_delete_bank_2),
                " ",
                getString(R.string.continue_confirmation)
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
fun BankIconImpl(blz: String) {
    getIcon(blz)?.let {
        Image(painter = painterResource(id = it), contentDescription = null)
    } ?: run {
        Image(imageVector = Icons.Filled.AccountBalance, contentDescription = null)
    }
}

fun getIcon(blz: String) = when {
    blz == "12030000" -> RF.drawable.dkb
    blz == "43060967" -> RF.drawable.gls
    blz.startsWith("200411") -> RF.drawable.comdirect
    blz[3] == '5' -> RF.drawable.sparkasse
    blz[3] == '9' -> RF.drawable.volksbank
    else -> null
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
        BankIconImpl(blz = bank.blz)
        Column {
            Text(bank.bankName)
            Text(bank.blz + " / " + bank.bic)
            Text(bank.userId)
        }
    }
    val menu = Menu(
        buildList {
            add(MenuEntry.delete("DELETE_BANK") { onDelete(bank) })
            add(
                MenuEntry(
                    command = "LIST_ACCOUNTS",
                    label = R.string.accounts,
                    icon = Icons.Filled.Checklist
                ) { onShow(bank) })
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
fun Loading(text: String? = "Loading") {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        CircularProgressIndicator()
        Text(text ?: stringResource(id = R.string.loading))
    }
}
