package org.totschnig.myexpenses.activity

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.dropbox.core.android.Auth
import eltos.simpledialogfragment.list.SimpleListDialog
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.util.crashreporting.CrashHandler
import org.totschnig.myexpenses.viewmodel.DropboxSetupViewModel

const val APP_KEY = "09ctg08r5gnsh5c"

class DropboxSetup: AbstractSyncBackup() {
    lateinit var viewModel: DropboxSetupViewModel
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            Auth.startOAuth2Authentication(this, APP_KEY)
        }

        viewModel = ViewModelProviders.of(this).get(DropboxSetupViewModel::class.java)
        viewModel.folderList.observe(this, Observer {
            if (it.size > 0) {
                showSelectFolderDialog(it)
            } else {
                showCreateFolderDialog()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        val authToken = Auth.getOAuth2Token()
        if (authToken != null) {
            viewModel.loadDropboxRootFolders(authToken)
        }
    }

    override fun onFolderSelect(extras: Bundle) {
        success(extras.getString(SimpleListDialog.SELECTED_SINGLE_LABEL))
    }

    override fun onFolderCreate(label: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun success(folderName: String?) {
        folderName?.let {
            val intent = Intent()
            val bundle = Bundle(2)
            bundle.putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, folderName)
            intent.putExtra(AccountManager.KEY_USERDATA, bundle)
            intent.putExtra(AccountManager.KEY_AUTHTOKEN, Auth.getOAuth2Token())
            intent.putExtra(SyncBackendSetupActivity.KEY_SYNC_PROVIDER_ID, R.id.SYNC_BACKEND_DROPBOX)
            intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, folderName)
            setResult(RESULT_OK, intent)
        } ?: CrashHandler.report("Success called, but no folderName provided")
        finish()
    }
}