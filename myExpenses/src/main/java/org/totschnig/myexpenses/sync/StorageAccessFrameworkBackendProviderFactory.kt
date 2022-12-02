package org.totschnig.myexpenses.sync

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.annotation.Keep
import org.totschnig.myexpenses.activity.ProtectedFragmentActivity
import org.totschnig.myexpenses.activity.SYNC_LOCAL_BACKEND_SETUP_REQUEST


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

    //TODO
    override fun setupIntent(activity: ProtectedFragmentActivity): Intent? {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity.startActivityForResult(intent, SYNC_LOCAL_BACKEND_SETUP_REQUEST)
        return null
    }

}