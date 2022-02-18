package org.totschnig.myexpenses.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.viewmodel.SyncViewModel

class SetupSyncDialogFragment : ComposeBaseDialogFragment() {

    data class AccountRow(
        val label: String,
        val uuid: String,
        val isLocal: Boolean,
        val isRemote: Boolean
    )

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
                AccountRow(item = it, linkState = mutableStateOf(false))
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) =
        initBuilder().setPositiveButton("Setup", null).create()

    @Composable
    fun AccountRow(
        item: AccountRow,
        linkState: MutableState<Boolean>
    ) {
        Row {
            Column(modifier = cell(0)) {
                Text(text = item.label, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text(text = item.uuid, maxLines = 1, overflow = TextOverflow.Ellipsis, fontSize = 10.sp)
            }
            if (item.isLocal) {
                Icon(
                    modifier = cell(1),
                    painter = painterResource(id = R.drawable.ic_menu_done),
                    contentDescription = "Local"
                )
            } else {
                Spacer(modifier = cell(1))
            }
            Icon(
                modifier = cell(2).clickable {
                    linkState.value = !linkState.value
                },
                painter = painterResource(id = if (linkState.value) R.drawable.ic_hchain else R.drawable.ic_hchain_broken),
                contentDescription = stringResource(id = R.string.menu_sync_link)
            )
            if (item.isRemote) {
                Icon(
                    modifier = cell(3),
                    painter = painterResource(id = R.drawable.ic_menu_done),
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
        fun newInstance(data: SyncViewModel.SyncAccountData) = SetupSyncDialogFragment().apply {
            arguments = Bundle().apply {
                putParcelable(KEY_DATA, data)
            }
        }
    }
}