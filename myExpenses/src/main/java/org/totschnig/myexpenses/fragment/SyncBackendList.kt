package org.totschnig.myexpenses.fragment

import android.app.Activity.RESULT_OK
import android.content.ContentResolver
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.*
import android.view.ContextMenu.ContextMenuInfo
import android.widget.ExpandableListView
import android.widget.ExpandableListView.*
import androidx.core.util.Pair
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.google.android.material.snackbar.BaseTransientBottomBar
import com.google.android.material.snackbar.Snackbar
import eltos.simpledialogfragment.SimpleDialog
import eltos.simpledialogfragment.SimpleDialog.OnDialogResultListener
import icepick.Icepick
import icepick.State
import org.totschnig.myexpenses.MyApplication
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.activity.SyncBackendSetupActivity.Companion.REQUEST_CODE_RESOLUTION
import org.totschnig.myexpenses.adapter.SyncBackendAdapter
import org.totschnig.myexpenses.databinding.SyncBackendsListBinding
import org.totschnig.myexpenses.dialog.AccountMetaDataDialogFragment
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.DialogUtils
import org.totschnig.myexpenses.dialog.MessageDialogFragment
import org.totschnig.myexpenses.feature.Feature
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.CurrencyContext
import org.totschnig.myexpenses.preference.PrefHandler
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.provider.TransactionProvider
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.activateSync
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.SyncBackendProvider
import org.totschnig.myexpenses.util.UiUtils
import org.totschnig.myexpenses.util.licence.LicenceHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.AbstractSyncBackendViewModel
import javax.inject.Inject

class SyncBackendList : Fragment(), OnGroupExpandListener, OnDialogResultListener,
    OnChildClickListener {
    private var _binding: SyncBackendsListBinding? = null
    private val binding get() = _binding!!
    private lateinit var syncBackendAdapter: SyncBackendAdapter
    private var metadataLoadingCount = 0
    private lateinit var snackbar: Snackbar
    private lateinit var viewModel: AbstractSyncBackendViewModel

    @Inject
    lateinit var prefHandler: PrefHandler

    @Inject
    lateinit var currencyContext: CurrencyContext

    @Inject
    lateinit var modelClass: Class<out AbstractSyncBackendViewModel>

    @Inject
    lateinit var licenceHandler: LicenceHandler

    @JvmField
    @State
    var resolutionPendingForGroup = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        val appComponent = (requireActivity().application as MyApplication).appComponent
        appComponent.inject(this)
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        viewModel = ViewModelProvider(this)[modelClass]
        appComponent.inject(viewModel)
        Icepick.restoreInstanceState(this, savedInstanceState)
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
        BackendService.forAccount(account)?.feature

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = SyncBackendsListBinding.inflate(inflater, container, false)
        syncBackendAdapter = SyncBackendAdapter(requireContext(), currencyContext, accountList)
        binding.list.setAdapter(syncBackendAdapter)
        binding.list.emptyView = binding.empty
        binding.list.setOnGroupExpandListener(this)
        binding.list.setOnChildClickListener(this)
        snackbar = Snackbar.make(
            binding.list,
            R.string.sync_loading_accounts_from_backend,
            BaseTransientBottomBar.LENGTH_INDEFINITE
        )
        UiUtils.increaseSnackbarMaxLines(snackbar)
        viewModel.getLocalAccountInfo()
            .observe(viewLifecycleOwner) { syncBackendAdapter.setLocalAccountInfo(it) }
        viewModel.loadLocalAccountInfo()
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
            menu.add(Menu.NONE, R.id.SYNC_REMOVE_BACKEND_COMMAND, 0, R.string.menu_remove)
            if (syncBackendAdapter.isEncrypted(packedPosition)) {
                menu.add(Menu.NONE, R.id.SHOW_PASSWORD_COMMAND, 0, R.string.input_label_passphrase)
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
                            R.string.menu_remove
                        )
                        putInt(
                            ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL,
                            android.R.string.cancel
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
        }
        return super.onContextItemSelected(item)
    }

    private fun requestSync(packedPosition: Long) {
        val syncAccountName = syncBackendAdapter.getSyncAccountName(packedPosition)
        val account = getAccount(syncAccountName)
        if (ContentResolver.getIsSyncable(account, TransactionProvider.AUTHORITY) > 0) {
            GenericAccountService.requestSync(syncAccountName)
        } else {
            val bundle = Bundle(1)
            bundle.putString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME, syncAccountName)
            SimpleDialog.build()
                .msg("Backend is not ready to be synced")
                .pos("Activate again")
                .extra(bundle)
                .show(this, DIALOG_INACTIVE_BACKEND)
        }
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
            if (!snackbar.isShownOrQueued) {
                snackbar.show()
            }
            val backendLabel = syncBackendAdapter.getBackendLabel(groupPosition)
            if (featureForAccount(backendLabel)?.let {
                    manageSyncBackends.isFeatureAvailable(it)
                } != false) {
                viewModel.accountMetadata(backendLabel).observe(viewLifecycleOwner) { result ->
                    metadataLoadingCount--
                    if (metadataLoadingCount == 0) {
                        snackbar.dismiss()
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
    }

    private fun handleAuthException(throwable: Throwable) =
        (throwable as? SyncBackendProvider.AuthException)?.resolution?.let {
            startActivityForResult(it, REQUEST_CODE_RESOLUTION)
            true
        } ?: false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        Icepick.saveInstanceState(this, outState)
    }

    fun getAccountForSync(packedPosition: Long): Account? {
        return syncBackendAdapter.getAccountForSync(packedPosition)
    }

    override fun onResult(dialogTag: String, which: Int, extras: Bundle): Boolean {
        if (dialogTag == DIALOG_INACTIVE_BACKEND && which == OnDialogResultListener.BUTTON_POSITIVE) {
            activateSync(
                getAccount(extras.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)!!),
                prefHandler
            )
        }
        return false
    }

    fun syncUnlink(uuid: String) {
        viewModel.syncUnlink(uuid).observe(this) { result ->
            result.onFailure {
                manageSyncBackends.showSnackBar(it.safeMessage)
            }
        }
    }

    companion object {
        private const val DIALOG_INACTIVE_BACKEND = "inactive_backend"
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