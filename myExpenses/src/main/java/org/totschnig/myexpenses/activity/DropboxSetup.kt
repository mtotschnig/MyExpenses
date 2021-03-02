package org.totschnig.myexpenses.activity

import android.accounts.AccountManager
import android.content.Intent
import android.os.Bundle
import androidx.lifecycle.ViewModelProvider
import com.dropbox.core.android.Auth
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.viewmodel.DropboxSetupViewModel

const val APP_KEY = "09ctg08r5gnsh5c"

class DropboxSetup : AbstractSyncBackup<DropboxSetupViewModel>() {
    private var oauthStartPending: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) {
            oauthStartPending = true
        }
    }

    override fun instantiateViewModel() = ViewModelProvider(this).get(DropboxSetupViewModel::class.java)

    override fun onResume() {
        super.onResume()
        if (oauthStartPending) {
            Auth.startOAuth2Authentication(this, APP_KEY)
            oauthStartPending = false
        } else {
            Auth.getOAuth2Token()?.also {
                viewModel.initWithAuthToken(it)
                if (!loadFinished) {
                    viewModel.query()
                }
            } ?: kotlin.run { abort() }
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