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
class LocalFileBackendProviderFactory : SyncBackendProviderFactory() {
    override fun fromAccount(
        context: Context,
        account: Account,
        accountManager: AccountManager
    ) = LocalFileBackendProvider(
        context,
        Uri.parse(accountManager.getUserData(account, GenericAccountService.KEY_SYNC_PROVIDER_URL))
    )

    override fun startSetup(activity: ProtectedFragmentActivity) {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity.startActivityForResult(intent, SYNC_LOCAL_BACKEND_SETUP_REQUEST)
    }

}