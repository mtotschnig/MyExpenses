package org.totschnig.myexpenses.activity

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProviders
import com.dropbox.core.android.Auth
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.viewmodel.DropboxSetupViewModel

const val APP_KEY = "09ctg08r5gnsh5c"

class DropboxSetup : AbstractSyncBackup<DropboxSetupViewModel>() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            Auth.startOAuth2Authentication(this, APP_KEY)
        }
    }

    override fun instantiateViewModel() = ViewModelProviders.of(this).get(DropboxSetupViewModel::class.java)

    override fun onResume() {
        super.onResume()
        val authToken = Auth.getOAuth2Token()
        if (authToken != null) {
            viewModel.initWithAuthToken(authToken)
            if (!loadFinished) {
                viewModel.query()
            }
        }
    }

    override fun buildSuccessIntent(folder: Pair<String, String>) = folder.second.let { name ->
        Intent().apply {
            putExtra(AccountManager.KEY_USERDATA, Bundle(1).apply {
                putString(GenericAccountService.KEY_SYNC_PROVIDER_URL, name)
            })
            putExtra(AccountManager.KEY_AUTHTOKEN, Auth.getOAuth2Token())
            putExtra(SyncBackendSetupActivity.KEY_SYNC_PROVIDER_ID, R.id.SYNC_BACKEND_DROPBOX)
            putExtra(AccountManager.KEY_ACCOUNT_NAME, name)
        }
    }

    override fun handleException(exception: Exception) = false
}