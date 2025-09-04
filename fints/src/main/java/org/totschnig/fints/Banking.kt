package org.totschnig.fints

import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.TextUtils
import android.widget.TextView
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
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
import androidx.compose.ui.platform.LocalAutofillManager
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import org.kapott.hbci.structures.Konto
import org.totschnig.fints.BankingViewModel.WorkState.*
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.compose.AppTheme
import org.totschnig.myexpenses.compose.DenseTextField
import org.totschnig.myexpenses.compose.HierarchicalMenu
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.UiText
import org.totschnig.myexpenses.compose.rememberMutableStateMapOf
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.model2.Bank
import java.time.LocalDate
import org.totschnig.fints.R as RF

class Banking : ProtectedFragmentActivity() {

    private val viewModel: BankingViewModel by viewModels()

    enum class DialogState {
        NoShow, Credentials, CredentialsForSync, Loading, AccountSelection, Done
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        DaggerFinTSComponent.builder().appComponent((application as MyApplication).appComponent)
            .build().inject(viewModel)
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.instMessage
                    .filterNotNull()
                    .collect {
                        showDismissibleSnackBar(it, object : Snackbar.Callback() {
                            override fun onDismissed(transientBottomBar: Snackbar?, event: Int) {
                                if (event == DISMISS_EVENT_SWIPE || event == DISMISS_EVENT_ACTION ||
                                    event == DISMISS_EVENT_TIMEOUT
                                )
                                    viewModel.messageShown()
                            }
                        })
                    }
            }
        }
        setContent {
            AppTheme {

                val data = viewModel.banks.collectAsState()
                var dialogState: DialogState by rememberSaveable {
                    mutableStateOf(if (calledFromOnboarding) DialogState.Credentials else DialogState.NoShow)
                }
                val bankingCredentials: MutableState<BankingCredentials> =
                    rememberSaveable { mutableStateOf(BankingCredentials.EMPTY) }

                val tanRequested = viewModel.tanRequested.observeAsState()
                val tanMediumRequested = viewModel.tanMediumRequested.observeAsState()
                val pushTanRequested = viewModel.pushTanRequested.observeAsState()
                val secMechRequested = viewModel.secMechRequested.observeAsState()

                val migrationDialogShown = rememberSaveable { mutableStateOf<Bank?>(null) }

                val workState = viewModel.workState.collectAsState()
                LaunchedEffect(workState.value) {
                    when (workState.value) {
                        is Initial -> {
                            if (dialogState == DialogState.Loading) {
                                dialogState = DialogState.Credentials
                            }
                        }

                        is Loading -> {
                            dialogState = DialogState.Loading
                        }

                        is AccountsLoaded -> {
                            dialogState = DialogState.AccountSelection
                        }

                        is Done -> {
                            dialogState = DialogState.Done
                        }

                        else -> {}
                    }
                }
                val errorState = viewModel.errorState.collectAsState()

                if (!calledFromOnboarding) {
                    Scaffold(
                        containerColor = MaterialTheme.colorScheme.surface,
                        topBar = {
                            TopAppBar(
                                title = { Text("Banking") },
                                navigationIcon = {
                                    IconButton(onClick = { finish() }) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                                    dialogState = DialogState.Credentials
                                    bankingCredentials.value = BankingCredentials.EMPTY
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
                                .padding(
                                    horizontal = dimensionResource(
                                        id = R.dimen.padding_main_screen
                                    )
                                )
                                .fillMaxSize()
                        ) {
                            if (data.value.isEmpty()) {
                                Text(
                                    text = stringResource(RF.string.no_bank_added_yet),
                                    modifier = Modifier.align(Alignment.Center)
                                )
                            } else {
                                LazyColumn {
                                    data.value.forEach { bank ->
                                        item {
                                            BankRow(
                                                bank = bank,
                                                onDelete = {
                                                    if (it.count > 0) {
                                                        confirmBankDelete(it)
                                                    } else {
                                                        viewModel.deleteBank(it)
                                                    }
                                                },
                                                onShow = {
                                                    dialogState = DialogState.Credentials
                                                    bankingCredentials.value =
                                                        BankingCredentials.fromBank(it)
                                                },
                                                onSync = {
                                                    dialogState = DialogState.CredentialsForSync
                                                    bankingCredentials.value =
                                                        BankingCredentials.fromBank(it)
                                                },
                                                onMigrate = if (bank.version == 1) {
                                                    { migrationDialogShown.value = it }
                                                } else null,
                                                onResetTanMechanism =
                                                    if (viewModel.hasStoredTanMech(bank.id)) {
                                                        { viewModel.resetTanMechanism(it.id) }
                                                    } else null
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                if (tanRequested.value == null && tanMediumRequested.value == null && pushTanRequested.value == null && secMechRequested.value == null) {
                    SetupDialog(dialogState, workState.value, bankingCredentials, errorState) {
                        if (calledFromOnboarding) {
                            setResult(if (it) RESULT_OK else RESULT_CANCELED)
                            finish()
                        } else {
                            dialogState = DialogState.NoShow
                            bankingCredentials.value = BankingCredentials.EMPTY
                            viewModel.reset()
                        }
                    }
                }
                TanDialog(tanRequested.value)
                TanMediaDialog(tanMediumRequested.value)
                PushTanDialog(pushTanRequested.value)
                SecMechDialog(secMechRequested.value)
                MigrationDialog(
                    migrationDialogShown,
                    onMigrate = { bank, passphrase -> viewModel.migrateBank(bank, passphrase) }
                )
            }
        }
    }

    @Composable
    private fun SetupDialog(
        dialogState: DialogState,
        workState: BankingViewModel.WorkState,
        bankingCredentials: MutableState<BankingCredentials>,
        errorState: State<String?>,
        dismiss: (success: Boolean) -> Unit,
    ) {
        when (dialogState) {
            DialogState.AccountSelection -> {
                val accounts = (workState as? AccountsLoaded)?.accounts
                    ?: return
                val selectedAccounts = rememberMutableStateMapOf<Int, Long>()
                var nrDays: Long? by remember { mutableStateOf(null) }
                val importMaxDuration = remember { derivedStateOf { nrDays == null } }

                val availableAccounts =
                    viewModel.accounts.collectAsState()
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
                AlertDialog(
                    properties = DialogProperties(dismissOnClickOutside = false),
                    onDismissRequest = { dismiss(false) },
                    icon = { DialogIcon() },
                    title = {
                        Text(
                            text = stringResource(id = RF.string.select_accounts),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                viewModel.importAccounts(
                                    bankingCredentials.value,
                                    workState.bank,
                                    workState.accounts.mapIndexedNotNull { index, pair ->
                                        selectedAccounts[index]?.let { pair.first to it }
                                    },
                                    nrDays?.let { LocalDate.now().minusDays(it) }
                                )
                            },
                            enabled = selectedAccounts.isNotEmpty()
                        ) {
                            Text(stringResource(R.string.menu_import))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { dismiss(false) }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.verticalScroll(rememberScrollState())
                        ) {
                            Error(errorMessage = errorState.value)
                            Help(buildList {
                                add(RF.string.select_accounts_help_1)
                                if (!calledFromOnboarding) {
                                    add(RF.string.select_accounts_help_2)
                                }
                                add(RF.string.select_accounts_help_3)
                            })

                            accounts.forEachIndexed { index, account ->
                                AccountRow(
                                    account.first,
                                    !account.second,
                                    targetOptions.value.find { it.first == selectedAccounts[index] }?.second,
                                    targetOptions.value.filterNot {
                                        it.first != 0L && selectedAccounts.values.contains(
                                            it.first
                                        )
                                    }
                                ) { selected, accountId ->
                                    if (selected) selectedAccounts[index] =
                                        accountId else selectedAccounts.remove(
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
                                            text = getString(RF.string.import_maximum),
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    Row(
                                        Modifier
                                            .fillMaxWidth()
                                            .selectable(
                                                selected = !importMaxDuration.value,
                                                onClick = {
                                                    if (nrDays == null) nrDays = 365
                                                },
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
                                                } catch (_: NumberFormatException) {
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
                                            text = parts.getOrElse(1) { "" },
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                            }
                        }
                    }
                )
            }

            DialogState.Credentials, DialogState.CredentialsForSync -> {
                val autofillManager = LocalAutofillManager.current
                AlertDialog(
                    properties = DialogProperties(dismissOnClickOutside = false),
                    onDismissRequest = { dismiss(false) },
                    icon = { DialogIcon() },
                    title = {
                        Text(
                            text = stringResource(id = if (bankingCredentials.value.isNew) RF.string.add_new_bank else RF.string.enter_pin),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                autofillManager?.commit()
                                if (dialogState == DialogState.CredentialsForSync)
                                    viewModel.syncAccount(bankingCredentials.value, null)
                                else
                                    viewModel.addBank(bankingCredentials.value)
                            },
                            enabled = bankingCredentials.value.isComplete
                        ) {
                            Text(
                                stringResource(RF.string.btn_load_accounts)
                            )
                        }
                    },
                    dismissButton = {
                        Button(onClick = { dismiss(false) }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                        ) {
                            Error(errorMessage = errorState.value)
                            Help(buildList {
                                add(RF.string.fints_intro_1)
                                if (calledFromOnboarding) {
                                    add(RF.string.fints_intro_2)
                                }
                            })
                            BankingCredentials(
                                bankingCredentials = bankingCredentials,
                                onDone = viewModel::addBank
                            )
                        }
                    }
                )
            }

            DialogState.Done -> {
                AlertDialog(
                    onDismissRequest = { dismiss(workState is Success) },
                    confirmButton = {
                        Button(
                            onClick = { dismiss(workState is Success) }
                        ) {
                            Text(stringResource(R.string.menu_close))
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier
                                .verticalScroll(rememberScrollState())
                        ) {
                            Error(errorMessage = errorState.value)
                            (workState as? Success)?.message?.let { Text(it) }
                        }
                    }
                )
            }

            DialogState.Loading -> {
                AlertDialog(
                    properties = DialogProperties(
                        dismissOnClickOutside = false,
                        dismissOnBackPress = false
                    ),
                    onDismissRequest = { },
                    confirmButton = { },
                    icon = { DialogIcon() },
                    text = {
                        Loading((workState as? Loading)?.message)
                    }
                )
            }

            DialogState.NoShow -> {}
        }
    }

    @Composable
    private fun DialogIcon() {
        Icon(
            imageVector = Icons.Filled.AccountBalance,
            contentDescription = null
        )
    }

    override fun dispatchCommand(command: Int, tag: Any?) =
        super.dispatchCommand(command, tag) || when (command) {
            R.id.DELETE_BANK_COMMAND_DO -> {
                viewModel.deleteBank(tag as Bank)
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
                bank
            ),
            null,
            MessageDialogFragment.noButton(), 0
        )
            .show(supportFragmentManager, "DELETE_ACCOUNT")
    }

    @Composable
    fun Help(@StringRes resIds: List<Int>) {
        val help = resIds.joinTo(SpannableStringBuilder(), " ") {
            getText(it)
        }
        AndroidView(
            modifier = Modifier
                .width(OutlinedTextFieldDefaults.MinWidth)
                .padding(bottom = 8.dp),
            factory = { context -> TextView(context) },
            update = { it.text = help }
        )
    }
}

@Composable
fun BankRow(
    bank: Bank,
    onDelete: (Bank) -> Unit = {},
    onShow: (Bank) -> Unit = {},
    onResetTanMechanism: ((Bank) -> Unit)? = null,
    onMigrate: ((Bank) -> Unit)? = null,
    onSync: (Bank) -> Unit = {}
) {
    val showMenu = rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = Modifier.clickable { showMenu.value = true },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        BankIconImpl(
            modifier = Modifier
                .padding(2.dp)
                .size(48.dp), bank = bank
        )
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
                ) { onShow(bank) }
            )
            add(
                MenuEntry(
                    command = "SYNC_ALL",
                    label = RF.string.menu_sync_account,
                    icon = Icons.Filled.Sync
                ) { onSync(bank) }
            )
            onMigrate?.let {
                add(
                    MenuEntry(
                        command = "MIGRATION",
                        label = UiText.StringValue("v1 -> v2")
                    ) { onMigrate(bank) }
                )
            }
            onResetTanMechanism?.let {
                add(
                    MenuEntry(
                        command = "RESET_TAN",
                        label = RF.string.reset_stored_configuration
                    ) { onResetTanMechanism(bank) }
                )
            }
        }
    )
    HierarchicalMenu(showMenu, menu)
}

@Composable
fun AccountRow(
    account: Konto,
    selectable: Boolean,
    selected: String?,
    targetOptions: List<Pair<Long, String>>,
    onSelectionChange: (Boolean, Long) -> Unit,
) {
    Row {
        if (selectable) {
            val showMenu = rememberSaveable { mutableStateOf(false) }
            Checkbox(checked = selected != null, onCheckedChange = {
                if (targetOptions.size > 1 && it) {
                    showMenu.value = true
                }
                onSelectionChange.invoke(it, 0)
            })
            if (showMenu.value)
                HierarchicalMenu(
                    expanded = showMenu, title = stringResource(id = RF.string.import_into),
                    menu = Menu(targetOptions.map {
                        MenuEntry(label = UiText.StringValue(it.second)) {
                            onSelectionChange.invoke(true, it.first)
                        }
                    })
                )
        } else {
            Icon(
                modifier = Modifier.width(48.dp),
                imageVector = Icons.Filled.Link,
                contentDescription = "Account is already imported"
            )
        }
        Column {
            Text(text = "${account.type} ${account.name}${account.name2?.let { " $it" } ?: ""}")
            Text(account.dbNumber)
            selected?.let { Text(stringResource(id = RF.string.import_into) + " " + it) }
        }
    }
}

@Preview
@Composable
private fun BankDemo() {
    Column {
        BankRow(
            bank = Bank(
                blz = "1234567",
                bic = "XPNSS",
                bankName = "My home bank",
                userId = "1234"
            )
        )
        BankRow(
            bank = Bank(
                blz = "200411",
                bic = "XPNSS",
                bankName = "Comdirect Bank",
                userId = "1234"
            )
        )
        BankRow(
            bank = Bank(
                blz = "1234567",
                bic = "XPNSS",
                bankName = "Sparda Bank",
                userId = "1234"
            )
        )
    }
}