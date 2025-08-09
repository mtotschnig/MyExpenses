package org.totschnig.myexpenses.dialog

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AlertDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.LinkOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.os.BundleCompat
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
import org.totschnig.myexpenses.compose.conditional
import org.totschnig.myexpenses.dialog.SetupSyncDialogFragment.AccountRow
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

    val data: SyncViewModel.SyncAccountData by lazy {
        BundleCompat.getParcelable(requireArguments(), KEY_DATA, SyncViewModel.SyncAccountData::class.java)!!
    }

    @Composable
    override fun BuildContent() {
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        modifier = Modifier.weight(LABEL_WEIGHT),
                        text = stringResource(id = R.string.account) + " (UUID)",
                        fontWeight = FontWeight.Bold
                    )
                    Text(modifier = Modifier.weight(SOURCE_WEIGHT), text = "Local", fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.width(LocalMinimumInteractiveComponentSize.current))
                    Text(modifier = Modifier.weight(SOURCE_WEIGHT), text = "Remote", fontWeight = FontWeight.Bold)
                }
                HorizontalDivider()
            }
            items(accountRows) { item ->
                val linkState = viewModel.dialogState[item.uuid]
                Account(item = item, linkState) {
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
                HorizontalDivider()
            }

            item {
                when (progress.value) {
                    SetupProgress.NOT_STARTED -> {
                        ButtonRow(modifier = Modifier.padding(top = 8.dp)) {
                            TextButton(onClick = { dismiss() }) {
                                Text(stringResource(id = android.R.string.cancel))
                            }
                            if (viewModel.dialogState.values.any { it != null }) {
                                TextButton(onClick = {
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
        accountRows = data.prepare()
    }

    override fun initBuilder(): AlertDialog.Builder = super.initBuilder().setCancelable(false)

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
                val account = BundleCompat.getParcelable(extras, KEY_DATA, AccountRow::class.java)!!
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

@Composable
private fun Account(
    item: AccountRow,
    linkState: SyncSource?,
    onLinkClick: (() -> Unit)
) {
    Column {

        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(LABEL_WEIGHT)) {
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
                    modifier = Modifier.weight(SOURCE_WEIGHT),
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
                Spacer(modifier = Modifier.weight(SOURCE_WEIGHT))
            }
            if (!item.isSealed) {
                IconButton(onLinkClick) {
                    Icon(
                        modifier = Modifier.conditional(linkState != SyncSource.COMPLETED) {
                            clickable(onClick = onLinkClick)
                        },

                        imageVector = if (linkState != null) Icons.Filled.Link else Icons.Filled.LinkOff,
                        contentDescription = stringResource(id = R.string.menu_sync_link)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(LocalMinimumInteractiveComponentSize.current))
            }
            if (item.isRemote) {
                Icon(
                    modifier = Modifier.weight(SOURCE_WEIGHT),
                    painter = painterResource(id = if (linkState == SyncSource.LOCAL) R.drawable.ic_menu_delete else R.drawable.ic_menu_done),
                    tint = if (linkState == SyncSource.REMOTE) Color.Green else
                        LocalContentColor.current,
                    contentDescription = "Remote"
                )
            } else {
                Spacer(modifier = Modifier.weight(SOURCE_WEIGHT))
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

@Preview(widthDp = 246)
@Composable
fun AccountPreview() {
    Account(AccountRow("Test-Level-${Build.VERSION.SDK_INT}", "Test-UUID",
        isLocal = true,
        isRemote = true,
        isSealed = false
    ), null) { }
}

private const val LABEL_WEIGHT = 3f
private const val SOURCE_WEIGHT = 1f