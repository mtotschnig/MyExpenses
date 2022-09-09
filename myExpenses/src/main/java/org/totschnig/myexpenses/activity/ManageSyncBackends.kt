package org.totschnig.myexpenses.activity

import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ExpandableListView.ExpandableListContextMenuInfo
import icepick.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.SetupSyncDialogFragment
import org.totschnig.myexpenses.fragment.SyncBackendList
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.AccountSealedException
import org.totschnig.myexpenses.viewmodel.SyncViewModel.SyncAccountData
import java.io.Serializable

class ManageSyncBackends : SyncBackendSetupActivity(), ContribIFace {

    @JvmField
    @State
    var incomingAccountDeleted = false
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.manage_sync_backends)
        setupToolbar(true)
        setTitle(R.string.pref_manage_sync_backends_title)
        if (savedInstanceState == null) {
            if (!licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION)) {
                contribFeatureRequested(ContribFeature.SYNCHRONIZATION, null)
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sync_backend, menu)
        menu.findItem(R.id.CREATE_COMMAND).subMenu?.let { addSyncProviderMenuEntries(it) }
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (getBackendServiceById(item.itemId) != null) {
            contribFeatureRequested(ContribFeature.SYNCHRONIZATION, item.itemId)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPositive(args: Bundle, checked: Boolean) {
        when (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
            R.id.SYNC_UNLINK_COMMAND -> {
                listFragment.syncUnlink(args.getString(DatabaseConstants.KEY_UUID)!!)
                return
            }
            R.id.SYNC_REMOVE_BACKEND_COMMAND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    if (viewModel.removeBackend(args.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)!!)) {
                        listFragment.reloadAccountList()
                    }
                } else {
                    CrashHandler.report(IllegalStateException("Remove backend not supported on API 21"))
                }
                return
            }
            R.id.SYNC_LINK_COMMAND_LOCAL_DO -> {
                val account = args.getSerializable(KEY_ACCOUNT) as Account
                viewModel.syncLinkLocal(
                    accountName = account.syncAccountName,
                    uuid = account.uuid!!
                ).observe(this) { result ->
                    result.onFailure {
                        showSnackBar(
                            if (it is AccountSealedException) getString(R.string.object_sealed) else it.safeMessage
                        )
                    }
                }
                return
            }
            R.id.SYNC_LINK_COMMAND_REMOTE_DO -> {
                val account = args.getSerializable(KEY_ACCOUNT) as Account
                if (account.uuid == intent.getStringExtra(DatabaseConstants.KEY_UUID)) {
                    incomingAccountDeleted = true
                }
                viewModel.syncLinkRemote(account).observe(this) { result ->
                    result.onFailure {
                        if (it is AccountSealedException) {
                            showSnackBar(R.string.object_sealed_debt)
                        }
                    }
                }
                return
            }
            else -> super.onPositive(args, checked)
        }
    }

    private fun finishWithIncomingAccountDeleted(): Boolean {
        if (incomingAccountDeleted) {
            setResult(RESULT_FIRST_USER)
            finish()
            return true
        }
        return false
    }

    override fun doHome() {
        if (!finishWithIncomingAccountDeleted()) {
            super.doHome()
        }
    }

    override fun onBackPressed() {
        if (!finishWithIncomingAccountDeleted()) {
            super.onBackPressed()
        }
    }

    override fun dispatchCommand(command: Int, tag: Any?): Boolean {
        if (super.dispatchCommand(command, tag)) {
            return true
        }
        when (command) {
            R.id.SYNC_LINK_COMMAND_LOCAL -> {
                val b = Bundle()
                b.putString(
                    ConfirmationDialogFragment.KEY_MESSAGE,
                    getString(R.string.dialog_confirm_sync_link_local)
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                    R.id.SYNC_LINK_COMMAND_LOCAL_DO
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                    R.string.dialog_command_sync_link_local
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL,
                    android.R.string.cancel
                )
                b.putSerializable(KEY_ACCOUNT, tag as Account?)
                ConfirmationDialogFragment.newInstance(b)
                    .show(supportFragmentManager, "SYNC_LINK_LOCAL")
                return true
            }
            R.id.SYNC_LINK_COMMAND_REMOTE -> {
                val b = Bundle()
                b.putString(
                    ConfirmationDialogFragment.KEY_MESSAGE,
                    getString(R.string.dialog_confirm_sync_link_remote)
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                    R.id.SYNC_LINK_COMMAND_REMOTE_DO
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                    R.string.dialog_command_sync_link_remote
                )
                b.putInt(
                    ConfirmationDialogFragment.KEY_NEGATIVE_BUTTON_LABEL,
                    android.R.string.cancel
                )
                b.putSerializable(KEY_ACCOUNT, tag as Account?)
                ConfirmationDialogFragment.newInstance(b)
                    .show(supportFragmentManager, "SYNC_LINK_REMOTE")
                return true
            }
            else -> return false
        }
    }


    override fun onReceiveSyncAccountData(data: SyncAccountData) {
        listFragment.reloadAccountList()
        if (callingActivity == null && (data.localAccountsNotSynced.isNotEmpty() || data.remoteAccounts.isNotEmpty())) {
            //if we were called from AccountEdit, we do not show the setup account selection
            //since we suppose that user wants to create one account for the account he is editing
            SetupSyncDialogFragment.newInstance(data).show(supportFragmentManager, "SETUP_SYNC")
        }
    }

    private val listFragment: SyncBackendList
        get() = supportFragmentManager.findFragmentById(R.id.backend_list) as SyncBackendList

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.SYNC_DOWNLOAD_COMMAND) {
            if (prefHandler.getBoolean(PrefKey.NEW_ACCOUNT_ENABLED, true)) {
                listFragment.getAccountForSync(
                    (item.menuInfo as ExpandableListContextMenuInfo).packedPosition
                )?.let { account ->
                    viewModel.save(account).observe(this) {
                        if (it == null) {
                            showSnackBar(
                                String.format(
                                    "There was an error saving account %s",
                                    account.label
                                )
                            )
                        }
                    }
                }
            } else {
                contribFeatureRequested(ContribFeature.ACCOUNTS_UNLIMITED, null)
            }
            return true
        }
        return super.onContextItemSelected(item)
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (tag is Int) {
            startSetup(tag)
        }
    }

    companion object {
        private const val KEY_ACCOUNT = "account"
    }
}