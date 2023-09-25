package org.totschnig.myexpenses.dialog

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
import org.totschnig.myexpenses.compose.*
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.SetupSyncViewModel
import org.totschnig.myexpenses.viewmodel.SetupSyncViewModel.SyncSource
import org.totschnig.myexpenses.viewmodel.SyncViewModel

class SetupSyncDialogFragment : ComposeBaseDialogFragment(), SimpleDialog.OnDialogResultListener {

    @Parcelize
    data class AccountRow(
        val label: String,
        val uuid: String,
        val isLocal: Boolean,
        val isRemote: Boolean,
        val isSealed: Boolean
    ) : Parcelable

    private val viewModel: SetupSyncViewModel by viewModels()

    private lateinit var accountRows: List<AccountRow>

    private fun SyncViewModel.SyncAccountData.prepare(): List<AccountRow> =
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
                        isRemote = remoteAccount != null,
                        isSealed = local.isSealed
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
                            isRemote = true,
                            isSealed = false
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

        LazyColumn(
            modifier = Modifier
                .padding(dialogPadding)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleLarge,
                        text = data.accountName
                    )
                    OverFlowMenu(
                        menu = Menu(
                            listOf(
                                MenuEntry(
                                    label = R.string.menu_help,
                                    command = "HELP"
                                ) {
                                    startActivity(
                                        Intent(
                                            requireContext(),
                                            Help::class.java
                                        ).apply {
                                            putExtra(
                                                HelpDialogFragment.KEY_CONTEXT,
                                                "SetupSync"
                                            )
                                            putExtra(
                                                HelpDialogFragment.KEY_TITLE,
                                                "${getString(R.string.synchronization)} - ${
                                                    getString(
                                                        R.string.setup
                                                    )
                                                }"
                                            )
                                        })
                                }
                            )
                        )
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
            }
            items(accountRows) {
                Account(item = it)
                Divider()
            }

            item {
                when (progress.value) {
                    SetupProgress.NOT_STARTED -> {
                        ButtonRow(modifier = Modifier.padding(top = 8.dp)) {
                            Button(onClick = { dismiss() }) {
                                Text(stringResource(id = android.R.string.cancel))
                            }
                            if (viewModel.dialogState.values.any { it != null }) {
                                Button(onClick = {
                                    progress.value = SetupProgress.RUNNING
                                    viewModel.setupSynchronization(
                                        accountName = data.accountName,
                                        localAccounts = data.localAccountsNotSynced.filter { account ->
                                            viewModel.dialogState.any {
                                                it.key == account.uuid && it.value == SyncSource.DEFAULT
                                            }
                                        },
                                        remoteAccounts = data.remoteAccounts.filter { account ->
                                            viewModel.dialogState.any {
                                                it.key == account.uuid() && it.value == SyncSource.DEFAULT
                                            }
                                        },
                                        conflicts = viewModel.dialogState.entries.filter {
                                            it.value == SyncSource.LOCAL || it.value == SyncSource.REMOTE
                                        }.map { entry ->
                                            Triple(
                                                data.localAccountsNotSynced.first { it.uuid == entry.key },
                                                data.remoteAccounts.first { it.uuid() == entry.key },
                                                entry.value!!
                                            )
                                        }
                                    ).observe(this@SetupSyncDialogFragment) { result ->
                                        result.onSuccess {
                                            if (!it) CrashHandler.report(IllegalStateException("setupSynchronization returned false "))
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
                        CircularProgressIndicator(modifier = Modifier.padding(top = 8.dp))
                    }

                    SetupProgress.COMPLETED -> {
                        Button(
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .fillMaxWidth()
                                .wrapContentWidth(align = Alignment.End),
                            onClick = { dismiss() }) {
                            Text(stringResource(id = android.R.string.ok))
                        }
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
    fun Account(item: AccountRow) {
        val linkState = viewModel.dialogState[item.uuid]
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
                        painter = painterResource(
                            id = when {
                                item.isSealed -> R.drawable.ic_lock
                                linkState == SyncSource.REMOTE -> R.drawable.ic_menu_delete
                                else -> R.drawable.ic_menu_done
                            }
                        ),
                        tint = if (linkState == SyncSource.LOCAL) Color.Green else
                            LocalContentColor.current,
                        contentDescription = "Local"
                    )
                } else {
                    Spacer(modifier = cell(1))
                }
                if (!item.isSealed) {
                    Icon(
                        modifier = cell(2).conditional(linkState != SyncSource.COMPLETED) {
                            clickable {
                                if (linkState == null) {
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
                                            .show(
                                                this@SetupSyncDialogFragment,
                                                SYNC_CONFLICT_DIALOG
                                            )
                                    } else {
                                        viewModel.dialogState[item.uuid] = SyncSource.DEFAULT
                                    }
                                } else {
                                    viewModel.dialogState[item.uuid] = null
                                }
                            }
                        },
                        painter = painterResource(id = if (linkState != null) R.drawable.ic_hchain else R.drawable.ic_hchain_broken),
                        contentDescription = stringResource(id = R.string.menu_sync_link)
                    )
                } else {
                    Spacer(modifier = cell(2))
                }
                if (item.isRemote) {
                    Icon(
                        modifier = cell(3),
                        painter = painterResource(id = if (linkState == SyncSource.LOCAL) R.drawable.ic_menu_delete else R.drawable.ic_menu_done),
                        tint = if (linkState == SyncSource.REMOTE) Color.Green else
                            LocalContentColor.current,
                        contentDescription = "Remote"
                    )
                } else {
                    Spacer(modifier = cell(3))
                }
            }
        }
        val errorColor = colorResource(id = R.color.colorErrorDialog)
        when (linkState) {
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
                viewModel.dialogState[account.uuid] = when (which) {
                    SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> SyncSource.REMOTE
                    SimpleDialog.OnDialogResultListener.BUTTON_NEGATIVE -> SyncSource.LOCAL
                    else -> null
                }
            }
        }
        return true
    }

}