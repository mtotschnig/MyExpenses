package org.totschnig.myexpenses.dialog

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.DialogInterface.BUTTON_POSITIVE
import android.os.Bundle
import android.os.Parcelable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.LocalContentColor
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModelProvider
import eltos.simpledialogfragment.SimpleDialog
import kotlinx.parcelize.Parcelize
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.SyncViewModel

class SetupSyncDialogFragment : ComposeBaseDialogFragment(), SimpleDialog.OnDialogResultListener, DialogInterface.OnClickListener {

    enum class SyncSource {
        DEFAULT, LOCAL, REMOTE
    }

    @Parcelize
    data class AccountRow(
        val label: String,
        val uuid: String,
        val isLocal: Boolean,
        val isRemote: Boolean
    ): Parcelable

    private val dialogState: MutableMap<String, MutableState<SyncSource?>> = mutableMapOf()

    private lateinit var viewModel: SyncViewModel

    fun SyncViewModel.SyncAccountData.prepare(): List<AccountRow> =
        buildList {
            localAccountsNotSynced.forEach { local ->
                val remoteAccount = remoteAccounts?.find { remote -> remote.uuid() == local.uuid }
                add(
                    AccountRow(
                        label = remoteAccount?.let {
                            if (it.label() == local.label) local.label else
                                local.label.substring(0, 15) + "/" + it.label().substring(0, 15)
                        } ?: local.label,
                        uuid = local.uuid,
                        isLocal = true,
                        isRemote = remoteAccount != null
                    )
                )
            }
            remoteAccounts
                ?.filter { remote -> !localAccountsNotSynced.any { local -> local.uuid == remote.uuid() } }
                ?.forEach {
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

    @Composable
    override fun BuildContent() {
        val data: SyncViewModel.SyncAccountData = requireArguments().getParcelable(KEY_DATA)!!

        Column(
            modifier = Modifier
                .padding(8.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row {
                Text(modifier = cell(0), text = stringResource(id = R.string.account) + " (UUID)")
                Text(modifier = cell(1), text = "Local")
                Spacer(modifier = cell(2))
                Text(modifier = cell(3), text = "Remote")
            }
            data.prepare().forEach {
                val linkState: MutableState<SyncSource?> = rememberSaveable(it.uuid) {
                    mutableStateOf(null)
                }
                dialogState.putIfAbsent(it.uuid, linkState)
                AccountRow(item = it, linkState = linkState)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(requireActivity())[SyncViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        initBuilder().setPositiveButton("Setup", this).create()

    @Composable
    fun AccountRow(
        item: AccountRow,
        linkState: MutableState<SyncSource?>
    ) {
        Row {
            Column(modifier = cell(0)) {
                Text(text = item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = item.uuid, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
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
                modifier = cell(2).clickable {

                    if (linkState.value == null) {
                        if (item.isLocal && item.isRemote) {
                            SimpleDialog.build()
                                .title(R.string.menu_sync_link)
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

    @SuppressLint("ModifierFactoryExtensionFunction")
    fun RowScope.cell(index: Int) = Modifier.weight(arrayOf(3f, 1f, 0.5f, 1f)[index])

    companion object {
        private const val KEY_DATA = "data"
        private const val SYNC_CONFLICT_DIALOG = "syncConflictDialog"
        fun newInstance(data: SyncViewModel.SyncAccountData) = SetupSyncDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_DATA, data)
            }
        }
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if(dialogTag == SYNC_CONFLICT_DIALOG) {
            val account = extras.getParcelable<AccountRow>(KEY_DATA)!!
            when(which) {
                SimpleDialog.OnDialogResultListener.BUTTON_POSITIVE -> {
                    dialogState[account.uuid]?.value = SyncSource.REMOTE
                }
                SimpleDialog.OnDialogResultListener.BUTTON_NEGATIVE -> {
                    dialogState[account.uuid]?.value = SyncSource.LOCAL
                }
            }
        }
        return true
    }

    override fun onClick(dialog: DialogInterface?, which: Int) {
        if (which == BUTTON_POSITIVE) {
            val data: SyncViewModel.SyncAccountData = requireArguments().getParcelable(KEY_DATA)!!


            viewModel.setupSynchronization(data.accountName)
        }
    }
}