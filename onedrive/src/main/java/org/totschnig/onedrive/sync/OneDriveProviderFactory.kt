package org.totschnig.onedrive.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.os.Build
import androidx.annotation.Keep
import androidx.annotation.RequiresApi
import org.totschnig.myexpenses.sync.SyncBackendProviderFactory
import org.totschnig.myexpenses.sync.getSyncProviderUrl
import org.totschnig.onedrive.activity.OneDriveSetup

@Keep
@RequiresApi(Build.VERSION_CODES.N)
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