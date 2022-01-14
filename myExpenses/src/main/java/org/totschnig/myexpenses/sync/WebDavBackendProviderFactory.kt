package org.totschnig.myexpenses.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.app.Activity
import android.content.Context
import android.content.Intent
import org.totschnig.myexpenses.R
import org.totschnig.myexpenses.activity.ManageSyncBackends
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.dialog.SetupWebdavDialogFragment
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.util.Result
import java.io.Serializable

class WebDavBackendProviderFactory : SyncBackendProviderFactory() {
    @Throws(SyncParseException::class)
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = WebDavBackendProvider(context, account, accountManager)

    override val label = "WebDAV"

    override fun startSetup(activity: ProtectedFragmentActivity) {
        val webdavDialogFragment = SetupWebdavDialogFragment()
        webdavDialogFragment.isCancelable = false
        webdavDialogFragment.show(activity.supportFragmentManager, WEBDAV_SETUP)
    }

    override val id = R.id.SYNC_BACKEND_WEBDAV

    override fun getRepairIntent(activity: Activity?): Intent? = null

    override fun startRepairTask(activity: ManageSyncBackends?, data: Intent?) = false

    override fun handleRepairTask(mExtra: Serializable?): Result<*>? = null

    override fun init() {}

    companion object {
        const val WEBDAV_SETUP = "WEBDAV_SETUP"
    }

}