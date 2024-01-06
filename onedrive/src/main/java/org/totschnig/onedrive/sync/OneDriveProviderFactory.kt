package org.totschnig.onedrive.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import androidx.annotation.Keep
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.sync.getSyncProviderUrl
import org.totschnig.onedrive.activity.OneDriveSetup

@Keep
class OneDriveProviderFactory : SyncBackendProviderFactory() {
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = OneDriveBackendProvider(
        context,
        accountManager.getSyncProviderUrl(account)
    )

    override val setupActivityClass =  OneDriveSetup::class.java
}