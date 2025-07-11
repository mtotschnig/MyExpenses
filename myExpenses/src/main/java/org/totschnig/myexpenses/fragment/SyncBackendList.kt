package org.totschnig.myexpenses.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.ContextMenu
import android.view.ContextMenu.ContextMenuInfo
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ExpandableListView
import android.widget.ExpandableListView.ExpandableListContextMenuInfo
import android.widget.ExpandableListView.OnChildClickListener
import android.widget.ExpandableListView.OnGroupExpandListener
import android.widget.ExpandableListView.PACKED_POSITION_TYPE_CHILD
import android.widget.ExpandableListView.getPackedPositionGroup
import android.widget.ExpandableListView.getPackedPositionType
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.evernote.android.state.State
import com.evernote.android.state.StateSaver
import com.google.android.material.snackbar.Snackbar
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.activity.SyncBackendSetupActivity
import org.totschnig.myexpenses.activity.SyncBackendSetupActivity.Companion.REQUEST_CODE_RESOLUTION
import org.totschnig.myexpenses.adapter.SyncBackendAdapter
import org.totschnig.myexpenses.databinding.SyncBackendsListBinding
import org.totschnig.myexpenses.dialog.AccountMetaDataDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.sync.SyncBackendProvider
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.AbstractSyncBackendViewModel
import javax.inject.Inject

class SyncBackendList : Fragment(), OnGroupExpandListener,
    OnChildClickListener {
    private var _binding: SyncBackendsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var syncBackendAdapter: SyncBackendAdapter
    private var metadataLoadingCount = 0
    private lateinit var viewModel: AbstractSyncBackendViewModel

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var modelClass: Class<out AbstractSyncBackendViewModel>

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    @State
    var resolutionPendingForGroup = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        val appComponent = (requireActivity().application as MyApplication).appComponent
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(this)[modelClass]
        appComponent.inject(viewModel)
        StateSaver.restoreInstanceState(this, savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        accountList.mapNotNull { account ->
            featureForAccount(account.first)
        }.distinct().forEach {
            manageSyncBackends.requireFeature(it)
        }
    }

    private fun featureForAccount(account: String): Feature? =
        BackendService.forAccount(account).getOrNull()?.feature

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SyncBackendsListBinding.inflate(inflater, container, false)
        syncBackendAdapter = SyncBackendAdapter(requireContext(), currencyContext.homeCurrencyString, accountList)
        binding.list.setAdapter(syncBackendAdapter)
        binding.list.emptyView = binding.empty
        binding.list.setOnGroupExpandListener(this)
        binding.list.setOnChildClickListener(this)
        lifecycleScope.launchWhenStarted {
            viewModel.localAccountInfo.collect {
                syncBackendAdapter.setLocalAccountInfo(it)
            }
        }
        registerForContextMenu(binding.list)
        return binding.root
    }

    override fun onCreateContextMenu(menu: ContextMenu, v: View, menuInfo: ContextMenuInfo?) {
        val packedPosition = (menuInfo as ExpandableListContextMenuInfo).packedPosition
        val commandId: Int
        val titleId: Int
        val isSyncAvailable = licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION)
        if (getPackedPositionType(packedPosition) == PACKED_POSITION_TYPE_CHILD) {
            if (isSyncAvailable) {
                when (syncBackendAdapter.getSyncState(packedPosition)) {
                    SyncBackendAdapter.SyncState.SYNCED_TO_THIS -> {
                        commandId = R.id.SYNC_UNLINK_COMMAND
                        titleId = R.string.menu_sync_unlink
                    }
                    SyncBackendAdapter.SyncState.UNSYNCED -> {
                        commandId = R.id.SYNC_LINK_COMMAND
                        titleId = R.string.menu_sync_link
                    }
                    SyncBackendAdapter.SyncState.SYNCED_TO_OTHER -> {
                        commandId = R.id.SYNCED_TO_OTHER_COMMAND
                        titleId = R.string.menu_sync_link
                    }
                    SyncBackendAdapter.SyncState.UNKNOWN -> {
                        commandId = R.id.SYNC_DOWNLOAD_COMMAND
                        titleId = R.string.menu_sync_download
                    }
                    else -> {
                        commandId = 0
                        titleId = 0
                    }
                }
                if (commandId != 0) menu.add(Menu.NONE, commandId, 0, titleId)
            }
        } else {
            if (isSyncAvailable) {
                menu.add(Menu.NONE, R.id.SYNC_COMMAND, 0, R.string.menu_sync_now)
            }
            menu.add(Menu.NONE, R.id.SYNC_REMOVE_BACKEND_COMMAND, 0, R.string.remove)
            if (syncBackendAdapter.isEncrypted(packedPosition)) {
                menu.add(Menu.NONE, R.id.SHOW_PASSWORD_COMMAND, 0, R.string.input_label_passphrase)
            }
            if (
                BackendService.forAccount(
                    syncBackendAdapter.getBackendLabel(
                        getPackedPositionGroup(packedPosition)
                    )
                ).getOrNull()?.supportsReconfiguration == true
            ) {
                menu.add(Menu.NONE, R.id.RECONFIGURE_COMMAND, 0, R.string.menu_reconfigure)
            }
        }
        super.onCreateContextMenu(menu, v, menuInfo)
    }

    private val accountList: List<Pair<String, Boolean>>
        get() = viewModel.getAccounts(requireActivity())

    override fun onContextItemSelected(item: MenuItem): Boolean {
        val packedPosition = (item.menuInfo as ExpandableListContextMenuInfo).packedPosition
        val itemId = item.itemId
        manageSyncBackends.trackCommand(itemId)
        when (itemId) {
            R.id.SYNC_COMMAND -> {
                requestSync(packedPosition)
                return true
            }
            R.id.SYNC_UNLINK_COMMAND -> {
                val accountForSync = getAccountForSync(packedPosition)
                if (accountForSync != null) {
                    DialogUtils.showSyncUnlinkConfirmationDialog(
                        manageSyncBackends,
                        accountForSync.syncAccountName, accountForSync.uuid
                    )
                }
                return true
            }
            R.id.SYNCED_TO_OTHER_COMMAND -> {
                val account = getAccountForSync(packedPosition)
                if (account != null) {
                    manageSyncBackends.showMessage(
                        getString(R.string.dialog_synced_to_other, account.uuid)
                    )
                }
                return true
            }
            R.id.SYNC_LINK_COMMAND -> {
                val account = getAccountForSync(packedPosition)
                if (account != null) {
                    MessageDialogFragment.newInstance(
                        getString(R.string.menu_sync_link),
                        getString(R.string.dialog_sync_link, account.uuid),
                        MessageDialogFragment.Button(
                            R.string.dialog_command_sync_link_remote,
                            R.id.SYNC_LINK_COMMAND_REMOTE,
                            account
                        ),
                        MessageDialogFragment.nullButton(android.R.string.cancel),
                        MessageDialogFragment.Button(
                            R.string.dialog_command_sync_link_local,
                            R.id.SYNC_LINK_COMMAND_LOCAL,
                            account
                        )
                    )
                        .show(parentFragmentManager, "SYNC_LINK")
                }
                return true
            }
            R.id.SYNC_REMOVE_BACKEND_COMMAND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val syncAccountName = syncBackendAdapter.getSyncAccountName(packedPosition)
                    ConfirmationDialogFragment.newInstance(Bundle().apply {
                        putString(
                            ConfirmationDialogFragment.KEY_MESSAGE,
                            (getString(R.string.dialog_confirm_sync_remove_backend, syncAccountName)
                                    + " " + getString(R.string.continue_confirmation))
                        )
                        putInt(
                            ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                            R.id.SYNC_REMOVE_BACKEND_COMMAND
                        )
                        putInt(
                            ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                            R.string.remove
                        )
                        putString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, syncAccountName)
                    })
                        .show(parentFragmentManager, "SYNC_REMOVE_BACKEND")
                } else {
                    manageSyncBackends.showSnackBar("Account can be deleted from Android System Settings")
                }
                return true
            }
            R.id.SHOW_PASSWORD_COMMAND -> {
                viewModel.loadPassword(syncBackendAdapter.getSyncAccountName(packedPosition))
                    .observe(this) {
                        manageSyncBackends.showDismissibleSnackBar(
                            it ?: "Could not retrieve passphrase"
                        )
                    }
                return true
            }
            R.id.RECONFIGURE_COMMAND -> {
                manageSyncBackends.reconfigure(syncBackendAdapter.getSyncAccountName(packedPosition))
                return true
            }
        }
        return super.onContextItemSelected(item)
    }

    private fun requestSync(packedPosition: Long) {
        val syncAccountName = syncBackendAdapter.getSyncAccountName(packedPosition)
        manageSyncBackends.requestSync(syncAccountName)
    }

    fun reloadAccountList() {
        syncBackendAdapter.setAccountList(accountList)
        val count = syncBackendAdapter.groupCount
        for (i in 0 until count) {
            binding.list.collapseGroup(i)
        }
    }

    private val manageSyncBackends get() = requireActivity() as ManageSyncBackends

    override fun onGroupExpand(groupPosition: Int) {
        if (!syncBackendAdapter.hasAccountMetadata(groupPosition)) {
            metadataLoadingCount++
            (requireActivity() as SyncBackendSetupActivity).showLoadingSnackBar()
            val backendLabel = syncBackendAdapter.getBackendLabel(groupPosition)
            viewModel.accountMetadata(backendLabel, featureForAccount(backendLabel)?.let {
                manageSyncBackends.isFeatureAvailable(it)
            } != false)?.observe(viewLifecycleOwner) { result ->
                metadataLoadingCount--
                if (metadataLoadingCount == 0) {
                    (requireActivity() as SyncBackendSetupActivity).dismissSnackBar()
                }
                result.onSuccess { list ->
                    syncBackendAdapter.setAccountMetadata(groupPosition, list)
                }.onFailure { throwable ->
                    if (handleAuthException(throwable)) {
                        resolutionPendingForGroup = groupPosition
                    } else {
                        manageSyncBackends.showSnackBar(
                            throwable.safeMessage,
                            Snackbar.LENGTH_SHORT
                        )
                    }
                }
            }
        }
    }

    private fun handleAuthException(throwable: Throwable) =
        (throwable as? SyncBackendProvider.AuthException)?.resolution?.let {
            startActivityForResult(it, REQUEST_CODE_RESOLUTION)
            true
        } ?: false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        StateSaver.saveInstanceState(this, outState)
    }

    fun getAccountForSync(packedPosition: Long): Account? {
        return syncBackendAdapter.getAccountForSync(packedPosition)
    }

    fun syncUnlink(uuid: String) {
        viewModel.syncUnlink(uuid).observe(this) { result ->
            result.onFailure {
                manageSyncBackends.showSnackBar(it.safeMessage)
            }
        }
    }

    override fun onChildClick(
        parent: ExpandableListView?,
        v: View?,
        groupPosition: Int,
        childPosition: Int,
        id: Long
    ): Boolean {
        syncBackendAdapter.getMetaData(groupPosition, childPosition)?.let {
            AccountMetaDataDialogFragment.newInstance(it).show(parentFragmentManager, "META_DATA")
        }
        return true
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_RESOLUTION && resultCode == RESULT_OK) {
            if (resolutionPendingForGroup != -1) {
                onGroupExpand(resolutionPendingForGroup)
                resolutionPendingForGroup = -1
            }
        }
    }
}