package org.totschnig.webdav.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import androidx.annotation.Keep
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.webdav.dialog.WebdavSetup
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

    override fun setupIntent(activity: ProtectedFragmentActivity) = Intent(activity, WebdavSetup::class.java)

}