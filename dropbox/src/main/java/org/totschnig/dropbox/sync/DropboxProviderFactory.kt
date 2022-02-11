package org.totschnig.dropbox.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import org.totschnig.dropbox.activity.DropboxSetup
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.SYNC_BACKEND_SETUP_REQUEST
import org.totschnig.myexpenses.sync.GenericAccountService
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory

const val KEY_DBX_CREDENTIAL = "DbxCredential"

@Keep
class DropboxProviderFactory : SyncBackendProviderFactory() {
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = DropboxBackendProvider(
        context,
        accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL)
    )

    override fun startSetup(activity: ProtectedFragmentActivity) {
        activity.startActivityForResult(
            Intent(activity, DropboxSetup::class.java),
            SYNC_BACKEND_SETUP_REQUEST
        )
    }

}