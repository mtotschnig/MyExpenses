package org.totschnig.myexpenses.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.DropboxSetup
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.SYNC_BACKEND_SETUP_REQUEST
import org.totschnig.myexpenses.util.Result
import java.io.Serializable

const val KEY_DBX_CREDENTIAL = "DbxCredential"

class DropboxProviderFactory : SyncBackendProviderFactory() {
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = DropboxBackendProvider(
        context,
        accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL)
    )

    override val label = "Dropbox"

    override fun startSetup(activity: ProtectedFragmentActivity) {
        activity.startActivityForResult(
            Intent(activity, DropboxSetup::class.java),
            SYNC_BACKEND_SETUP_REQUEST
        )
    }

    override val id = R.id.SYNC_BACKEND_DROPBOX

    override fun getRepairIntent(activity: Activity?): Intent? = null

    override fun startRepairTask(activity: ManageSyncBackends?, data: Intent?) = false

    override fun handleRepairTask(mExtra: Serializable?): Result<*>? = null

    override fun init() {}
}