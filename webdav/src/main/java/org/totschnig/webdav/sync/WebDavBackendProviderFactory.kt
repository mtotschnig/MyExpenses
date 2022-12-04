package org.totschnig.webdav.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.annotation.Keep
import org.totschnig.myexpenses.sync.SyncBackendProvider.SyncParseException
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.webdav.dialog.WebdavSetup

@Keep
class WebDavBackendProviderFactory : SyncBackendProviderFactory() {
    @Throws(SyncParseException::class)
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = WebDavBackendProvider(context, account, accountManager)

    override val setupActivityClass = WebdavSetup::class.java

}