package org.totschnig.myexpenses.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ExpandableListView.ExpandableListContextMenuInfo
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.os.BundleCompat
import arrow.core.flatMap
import com.evernote.android.state.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.SetupSyncDialogFragment
import org.totschnig.myexpenses.fragment.SyncBackendList
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model2.Account
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.sync.BackendService
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory.Companion.ACTION_RECONFIGURE
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.util.safeMessage
import org.totschnig.myexpenses.viewmodel.AccountSealedException
import org.totschnig.myexpenses.viewmodel.SyncViewModel.SyncAccountData
import java.io.Serializable

class ManageSyncBackends : SyncBackendSetupActivity(), ContribIFace {

    private lateinit var onBackPressedCallback: OnBackPressedCallback

    private val reconfigure =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                result.data?.extras?.let {
                    syncViewModel.reconfigure(it).observe(this) { success ->
                        if (success) {
                            listFragment.reloadAccountList()
                        } else {
                            showSnackBar("Reconfiguration failed")
                        }
                    }
                }
            }
        }

    fun reconfigure(syncAccount: String) {
        BackendService.forAccount(syncAccount).flatMap { it.instantiate() }.onSuccess {
            reconfigure.launch(
                Intent(this, it.setupActivityClass).apply {
                    action = ACTION_RECONFIGURE
                    putExtras(syncViewModel.getReconfigurationData(syncAccount))
                }
            )
        }
    }

    @State
    var incomingAccountDeleted = false
    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupWithFragment(savedInstanceState == null, false) {
            SyncBackendList()
        }
        setupToolbar()
        setTitle(R.string.pref_manage_sync_backends_title)
        if (savedInstanceState == null) {
            if (!licenceHandler.hasTrialAccessTo(ContribFeature.SYNCHRONIZATION)) {
                contribFeatureRequested(ContribFeature.SYNCHRONIZATION)
            }
        }
        onBackPressedCallback = object : OnBackPressedCallback(incomingAccountDeleted) {
            override fun handleOnBackPressed() {
                finishWithIncomingAccountDeleted()
            }
        }
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)
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
        super.onPositive(args, checked)
        when (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
            R.id.SYNC_UNLINK_COMMAND -> {
                listFragment.syncUnlink(args.getString(DatabaseConstants.KEY_UUID)!!)
            }

            R.id.SYNC_REMOVE_BACKEND_COMMAND -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
                    val accountName = args.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)!!
                    if (syncViewModel.removeBackend(accountName)) {
                        listFragment.reloadAccountList()
                        if (prefHandler.cloudStorage == accountName) {
                            prefHandler.remove(PrefKey.AUTO_BACKUP_CLOUD)
                        }
                    }
                } else {
                    CrashHandler.report(IllegalStateException("Remove backend not supported on API 21"))
                }
            }

            R.id.SYNC_LINK_COMMAND_LOCAL_DO -> {
                BundleCompat.getSerializable(args, KEY_ACCOUNT, Account::class.java)?.let { account ->
                    syncViewModel.syncLinkLocal(
                        accountName = account.syncAccountName!!,
                        uuid = account.uuid!!
                    ).observe(this) { result ->
                        result.onFailure {
                            showSnackBar(
                                if (it is AccountSealedException) getString(R.string.object_sealed) else it.safeMessage
                            )
                        }
                    }
                }
            }

            R.id.SYNC_LINK_COMMAND_REMOTE_DO -> {
                BundleCompat.getSerializable(args, KEY_ACCOUNT, Account::class.java)?.let { account ->
                    if (account.uuid == intent.getStringExtra(DatabaseConstants.KEY_UUID)) {
                        incomingAccountDeleted = true
                        onBackPressedCallback.isEnabled = true
                    }
                    syncViewModel.syncLinkRemote(account).observe(this) { result ->
                        result.onFailure {
                            if (it is AccountSealedException) {
                                showSnackBar(R.string.object_sealed)
                            }
                        }
                    }
                }
            }
        }
    }

    private fun finishWithIncomingAccountDeleted() {
        setResult(RESULT_FIRST_USER)
        finish()
    }

    override fun doHome() {
        if (incomingAccountDeleted) {
            finishWithIncomingAccountDeleted()
        } else {
            super.doHome()
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
                b.putSerializable(KEY_ACCOUNT, tag as Account?)
                ConfirmationDialogFragment.newInstance(b)
                    .show(supportFragmentManager, "SYNC_LINK_REMOTE")
                return true
            }

            else -> return false
        }
    }


    override fun onReceiveSyncAccountData(data: SyncAccountData) {
        GenericAccountService.activateSync(data.accountName, prefHandler)
        listFragment.reloadAccountList()
        if (callingActivity == null && (data.localAccountsNotSynced.isNotEmpty() || data.remoteAccounts.isNotEmpty())) {
            //if we were called from AccountEdit, we do not show the setup account selection
            //since we suppose that user wants to create one account for the account he is editing
            SetupSyncDialogFragment.newInstance(data).show(supportFragmentManager, "SETUP_SYNC")
        }
    }

    private val listFragment: SyncBackendList
        get() = supportFragmentManager.findFragmentById(R.id.fragment_container) as SyncBackendList

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.SYNC_DOWNLOAD_COMMAND) {
            if (prefHandler.getBoolean(PrefKey.NEW_ACCOUNT_ENABLED, true)) {
                listFragment.getAccountForSync(
                    (item.menuInfo as ExpandableListContextMenuInfo).packedPosition
                )?.let { account ->
                    syncViewModel.save(account).observe(this) {
                        it.onFailure {
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
                contribFeatureRequested(ContribFeature.ACCOUNTS_UNLIMITED)
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