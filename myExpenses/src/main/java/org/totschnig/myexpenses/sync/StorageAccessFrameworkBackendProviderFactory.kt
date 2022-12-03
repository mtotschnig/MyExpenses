package org.totschnig.myexpenses.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.net.Uri
import androidx.annotation.Keep
import org.totschnig.myexpenses.activity.SafSetup


@Keep
class StorageAccessFrameworkBackendProviderFactory : SyncBackendProviderFactory() {

    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = StorageAccessFrameworkBackendProvider(
        context,
        Uri.parse(accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL))
    )

    override val setupActivityClass = SafSetup::class.java

}