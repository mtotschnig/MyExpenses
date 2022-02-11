package org.totschnig.webdav.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.annotation.Keep
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.webdav.dialog.SetupWebdavDialogFragment
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory

@Keep
class WebDavBackendProviderFactory : SyncBackendProviderFactory() {
    @Throws(SyncParseException::class)
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = WebDavBackendProvider(context, account, accountManager)

    override fun startSetup(activity: ProtectedFragmentActivity) {
        val webdavDialogFragment =
            SetupWebdavDialogFragment()
        webdavDialogFragment.isCancelable = false
        webdavDialogFragment.show(activity.supportFragmentManager, WEBDAV_SETUP)
    }

    companion object {
        const val WEBDAV_SETUP = "WEBDAV_SETUP"
    }

}