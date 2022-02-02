package org.totschnig.myexpenses.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.ExpandableListView.ExpandableListContextMenuInfo
import icepick.State
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.dialog.ConfirmationDialogFragment
import org.totschnig.myexpenses.dialog.select.SelectUnSyncedAccountDialogFragment
import org.totschnig.myexpenses.fragment.SyncBackendList
import org.totschnig.myexpenses.model.Account
import org.totschnig.myexpenses.model.ContribFeature
import org.totschnig.myexpenses.model.Model
import org.totschnig.myexpenses.preference.PrefKey
import org.totschnig.myexpenses.provider.DatabaseConstants
import org.totschnig.myexpenses.task.TaskExecutionFragment
import org.totschnig.myexpenses.util.Result
import org.totschnig.myexpenses.viewmodel.AccountSealedException
import org.totschnig.myexpenses.viewmodel.SyncViewModel.SyncAccountData
import java.io.Serializable

class ManageSyncBackends : SyncBackendSetupActivity(), ContribIFace {
    private var newAccount: Account? = null

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
            sanityCheck()
        }
    }

    private fun sanityCheck() {
        for (factory in backendProviders) {
            val repairIntent = factory.getRepairIntent(this)
            if (repairIntent != null) {
                startActivityForResult(repairIntent, REQUEST_REPAIR_INTENT)
                //for the moment we handle only one problem at one time
                break
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.sync_backend, menu)
        addSyncProviderMenuEntries(menu.findItem(R.id.CREATE_COMMAND).subMenu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (getSyncBackendProviderFactoryById(item.itemId) != null) {
            contribFeatureRequested(ContribFeature.SYNCHRONIZATION, item.itemId)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onPositive(args: Bundle, checked: Boolean) {
        when (args.getInt(ConfirmationDialogFragment.KEY_COMMAND_POSITIVE)) {
            R.id.SYNC_UNLINK_COMMAND -> {
                listFragment!!.syncUnlink(args.getString(DatabaseConstants.KEY_UUID)!!)
                return
            }
            R.id.SYNC_REMOVE_BACKEND_COMMAND -> {
                startTaskExecution(
                    TaskExecutionFragment.TASK_SYNC_REMOVE_BACKEND,
                    arrayOf(args.getString(DatabaseConstants.KEY_SYNC_ACCOUNT_NAME)),
                    null,
                    0
                )
                return
            }
            R.id.SYNC_LINK_COMMAND_LOCAL_DO -> {
                val account = args.getSerializable(KEY_ACCOUNT) as Account
                startTaskExecution(
                    TaskExecutionFragment.TASK_SYNC_LINK_LOCAL,
                    arrayOf(account.uuid!!),
                    account.syncAccountName,
                    0
                )
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
            R.id.TRY_AGAIN_COMMAND -> {
                sanityCheck()
                return true
            }
            else -> return false
        }
    }

    //DbWriteFragment
    override fun onPostExecute(result: Uri?) {
        super.onPostExecute(result)
        if (result == null) {
            showSnackBar(String.format("There was an error saving account %s", newAccount!!.label))
        }
    }

    override fun onReceiveSyncAccountData(data: SyncAccountData) {
        listFragment!!.reloadAccountList()
        if (data.localNotSynced > 0) {
            showSelectUnsyncedAccount(data.accountName)
        }
    }

    override fun onPostExecute(taskId: Int, o: Any?) {
        super.onPostExecute(taskId, o)
        when (taskId) {
            TaskExecutionFragment.TASK_SYNC_REMOVE_BACKEND -> {
                val result = o as Result<*>?
                if (result!!.isSuccess) {
                    listFragment!!.reloadAccountList()
                }
            }
            TaskExecutionFragment.TASK_SYNC_LINK_SAVE -> {
                run {
                    val result = o as Result<*>?
                    showDismissibleSnackBar(result!!.print(this))
                }
                run {
                    val result = o as Result<*>?
                    if (!result!!.isSuccess) {
                        showSnackBar(result.print(this))
                    }
                }
            }
            TaskExecutionFragment.TASK_SYNC_LINK_LOCAL -> {
                val result = o as Result<*>?
                if (!result!!.isSuccess) {
                    showSnackBar(result.print(this))
                }
            }
            TaskExecutionFragment.TASK_REPAIR_SYNC_BACKEND -> {
                val result = o as Result<*>?
                val resultPrintable = result!!.print(this)
                if (result.isSuccess) {
                    showSnackBar(resultPrintable)
                } else {
                    val b = Bundle()
                    b.putString(ConfirmationDialogFragment.KEY_MESSAGE, resultPrintable)
                    b.putInt(
                        ConfirmationDialogFragment.KEY_COMMAND_POSITIVE,
                        R.id.TRY_AGAIN_COMMAND
                    )
                    b.putInt(
                        ConfirmationDialogFragment.KEY_POSITIVE_BUTTON_LABEL,
                        R.string.button_label_try_again
                    )
                    ConfirmationDialogFragment.newInstance(b)
                        .show(supportFragmentManager, "REPAIR_SYNC_FAILURE")
                }
            }
        }
    }

    private fun showSelectUnsyncedAccount(accountName: String?) {
        //if we were called from AccountEdit, we do not show the unsynced account selection
        //since we suppose that user wants to create one account for the account he is editing
        if (callingActivity == null) {
            SelectUnSyncedAccountDialogFragment.newInstance(accountName)
                .show(supportFragmentManager, "SELECT_UNSYNCED")
        }
    }

    private val listFragment: SyncBackendList?
        get() = supportFragmentManager.findFragmentById(R.id.backend_list) as SyncBackendList?

    override fun onContextItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.SYNC_DOWNLOAD_COMMAND) {
            if (prefHandler.getBoolean(PrefKey.NEW_ACCOUNT_ENABLED, true)) {
                newAccount = listFragment!!.getAccountForSync(
                    (item.menuInfo as ExpandableListContextMenuInfo).packedPosition
                )
                if (newAccount != null) {
                    startDbWriteTask()
                }
            } else {
                contribFeatureRequested(ContribFeature.ACCOUNTS_UNLIMITED, null)
            }
            return true
        }
        return super.onContextItemSelected(item)
    }

    override fun getObject(): Model {
        return newAccount!!
    }

    override fun contribFeatureCalled(feature: ContribFeature, tag: Serializable?) {
        if (tag is Int) {
            startSetup((tag as Int?)!!)
        }
    }

    override fun contribFeatureNotCalled(feature: ContribFeature) {}
    public override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)
        if (resultCode == RESULT_OK) {
            if (requestCode == REQUEST_REPAIR_INTENT) {
                for (factory in backendProviders) {
                    if (factory.startRepairTask(this, intent)) {
                        break
                    }
                }
            }
        }
    }

    companion object {
        private const val REQUEST_REPAIR_INTENT = 1
        private const val KEY_ACCOUNT = "account"
    }
}