package org.totschnig.myexpenses.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.viewModels
import eltos.simpledialogfragment.SimpleDialog
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.Help
import org.totschnig.myexpenses.compose.ButtonRow
import org.totschnig.myexpenses.compose.Menu
import org.totschnig.myexpenses.compose.MenuEntry
import org.totschnig.myexpenses.compose.OverFlowMenu
import org.totschnig.myexpenses.viewmodel.SetupSyncViewModel
import org.totschnig.myexpenses.viewmodel.SetupSyncViewModel.SyncSource
import org.totschnig.myexpenses.viewmodel.SyncViewModel

class SetupSyncDialogFragment : ComposeBaseDialogFragment(), SimpleDialog.OnDialogResultListener {

    @Parcelize
    data class AccountRow(
        val label: String,
        val uuid: String,
        val isLocal: Boolean,
        val isRemote: Boolean
    ) : Parcelable

    private val viewModel: SetupSyncViewModel by viewModels()

    private lateinit var accountRows: List<AccountRow>

    fun SyncViewModel.SyncAccountData.prepare(): List<AccountRow> =
        buildList {
            localAccountsNotSynced.forEach { local ->
                val remoteAccount = remoteAccounts.find { remote -> remote.uuid() == local.uuid }
                add(
                    AccountRow(
                        label = remoteAccount?.let {
                            if (it.label() == local.label) local.label else
                                local.label.take(15) + "/" + it.label().take(15)
                        } ?: local.label,
                        uuid = local.uuid,
                        isLocal = true,
                        isRemote = remoteAccount != null
                    )
                )
            }
            remoteAccounts
                .filter { remote -> !localAccountsNotSynced.any { local -> local.uuid == remote.uuid() } }
                .forEach {
                    add(
                        AccountRow(
                            label = it.label(),
                            uuid = it.uuid(),
                            isLocal = false,
                            isRemote = true
                        )
                    )
                }
        }

    enum class SetupProgress {
        NOT_STARTED, RUNNING, COMPLETED
    }

    @Composable
    override fun BuildContent() {
        val data: SyncViewModel.SyncAccountData =
            requireArguments().getParcelable(KEY_DATA)!!
        val progress = remember {
            mutableStateOf(SetupProgress.NOT_STARTED)
        }
        Column(
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    style = MaterialTheme.typography.h6,
                    text = data.accountName
                )
                OverFlowMenu(
                    menu = Menu(
                        listOf(
                            MenuEntry(
                                label = stringResource(id = R.string.menu_help)
                            ) {
                                startActivity(Intent(requireContext(), Help::class.java).apply {
                                    putExtra(HelpDialogFragment.KEY_CONTEXT, "SetupSync")
                                    putExtra(
                                        HelpDialogFragment.KEY_TITLE,
                                        "${getString(R.string.synchronization)} - ${getString(R.string.setup)}"
                                    )
                                })
                            }
                        )
                    ), target = Unit
                )
            }
            Row {
                Text(
                    modifier = cell(0),
                    text = stringResource(id = R.string.account) + " (UUID)",
                    fontWeight = FontWeight.Bold
                )
                Text(modifier = cell(1), text = "Local", fontWeight = FontWeight.Bold)
                Spacer(modifier = cell(2))
                Text(modifier = cell(3), text = "Remote", fontWeight = FontWeight.Bold)
            }
            Divider()
            accountRows.forEach {
                Account(item = it, linkState = viewModel.dialogState.getOrPut(it.uuid) {
                    mutableStateOf(null)
                })
                Divider()
            }

            when (progress.value) {
                SetupProgress.NOT_STARTED -> {
                    ButtonRow {
                        Button(onClick = { dismiss() }) {
                            Text(stringResource(id = android.R.string.cancel))
                        }
                        if (viewModel.dialogState.values.any { it.value != null }) {
                            Button(onClick = {
                                progress.value = SetupProgress.RUNNING
                                viewModel.setupSynchronization(
                                    accountName = data.accountName,
                                    localAccounts = data.localAccountsNotSynced.filter { account ->
                                        viewModel.dialogState.any {
                                            it.key == account.uuid && it.value.value == SyncSource.DEFAULT
                                        }
                                    },
                                    remoteAccounts = data.remoteAccounts.filter { account ->
                                        viewModel.dialogState.any {
                                            it.key == account.uuid() && it.value.value == SyncSource.DEFAULT
                                        }
                                    },
                                    conflicts = viewModel.dialogState.entries.filter {
                                        it.value.value == SyncSource.LOCAL || it.value.value == SyncSource.REMOTE
                                    }.map { entry ->
                                        Triple(
                                            data.localAccountsNotSynced.first { it.uuid == entry.key },
                                            data.remoteAccounts.first { it.uuid() == entry.key },
                                            entry.value.value!!
                                        )
                                    }
                                ).observe(this@SetupSyncDialogFragment) {
                                    it.onSuccess {
                                        progress.value = SetupProgress.COMPLETED
                                    }
                                }
                            }) {
                                Text(stringResource(id = R.string.menu_sync_link))
                            }
                        }
                    }
                }
                SetupProgress.RUNNING -> {
                    CircularProgressIndicator()
                }
                SetupProgress.COMPLETED -> {
                    Button(modifier = Modifier.align(Alignment.End), onClick = { dismiss() }) {
                        Text(stringResource(id = android.R.string.ok))
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        (requireActivity().application as MyApplication).appComponent.inject(viewModel)
        accountRows =
            requireArguments().getParcelable<SyncViewModel.SyncAccountData>(KEY_DATA)!!.prepare()
    }

    override fun initBuilder(): AlertDialog.Builder = super.initBuilder().setCancelable(false)

    @Composable
    fun Account(
        item: AccountRow,
        linkState: MutableState<SyncSource?>
    ) {
        Column {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = cell(0)) {
                    Text(text = item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = item.uuid,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontSize = 10.sp
                    )
                }
                if (item.isLocal) {
                    Icon(
                        modifier = cell(1),
                        painter = painterResource(id = if (linkState.value == SyncSource.REMOTE) R.drawable.ic_menu_delete else R.drawable.ic_menu_done),
                        tint = if (linkState.value == SyncSource.LOCAL) Color.Green else
                            LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                        contentDescription = "Local"
                    )
                } else {
                    Spacer(modifier = cell(1))
                }
                Icon(
                    modifier = cell(2).then(
                        if (linkState.value == SyncSource.COMPLETED) Modifier else Modifier.clickable {

                            if (linkState.value == null) {
                                if (item.isLocal && item.isRemote) {
                                    SimpleDialog.build()
                                        .extra(Bundle().apply {
                                            putParcelable(KEY_DATA, item)
                                        })
                                        .msg(
                                            getString(R.string.dialog_sync_link, item.uuid)
                                        )
                                        .pos(R.string.dialog_command_sync_link_remote)
                                        .neut()
                                        .neg(R.string.dialog_command_sync_link_local)
                                        .show(this@SetupSyncDialogFragment, SYNC_CONFLICT_DIALOG)
                                } else {
                                    linkState.value = SyncSource.DEFAULT
                                }
                            } else {
                                linkState.value = null
                            }
                        },
                    ),
                    painter = painterResource(id = if (linkState.value != null) R.drawable.ic_hchain else R.drawable.ic_hchain_broken),
                    contentDescription = stringResource(id = R.string.menu_sync_link)
                )
                if (item.isRemote) {
                    Icon(
                        modifier = cell(3),
                        painter = painterResource(id = if (linkState.value == SyncSource.LOCAL) R.drawable.ic_menu_delete else R.drawable.ic_menu_done),
                        tint = if (linkState.value == SyncSource.REMOTE) Color.Green else
                            LocalContentColor.current.copy(alpha = LocalContentAlpha.current),
                        contentDescription = "Remote"
                    )
                } else {
                    Spacer(modifier = cell(3))
                }
            }
        }
        val errorColor = colorResource(id = R.color.colorErrorDialog)
        when (linkState.value) {
            SyncSource.LOCAL -> errorColor to R.string.dialog_confirm_sync_link_local
            SyncSource.REMOTE -> errorColor to R.string.dialog_confirm_sync_link_remote
            SyncSource.COMPLETED -> Color.Green to R.string.setup_completed
            else -> null
        }?.let { (color, msg) ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_warning),
                    contentDescription = null,
                    tint = color
                )
                Text(
                    color = color,
                    text = stringResource(id = msg),
                    fontSize = 12.sp
                )
            }
        }
    }

    @SuppressLint("ModifierFactoryExtensionFunction")
    fun RowScope.cell(index: Int) = Modifier.weight(arrayOf(3f, 1f, 0.5f, 1f)[index])

    companion object {
        private const val KEY_DATA = "data"
        private const val SYNC_CONFLICT_DIALOG = "syncConflictDialog"
        fun newInstance(data: SyncViewModel.SyncAccountData) = SetupSyncDialogFragment().apply {
            isCancelable = false
            arguments = Bundle().apply {
                putParcelable(KEY_DATA, data)
            }
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        when (dialogTag) {
            SYNC_CONFLICT_DIALOG -> {
                val account = extras.getParcelable<AccountRow>(KEY_DATA)!!
                viewModel.dialogState[account.uuid]?.value = when (which) {
                    SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> SyncSource.REMOTE
                    SimpleDialog.OnDialogResultListener.BUTTON_NEGATIVE -> SyncSource.LOCAL
                    else -> null
                }
            }
        }
        return true
    }

}