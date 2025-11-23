package org.totschnig.myexpenses.service

import android.app.IntentService
import android.content.Intent
import org.totschnig.myexpenses.provider.KEY_SYNC_ACCOUNT_NAME
import org.totschnig.myexpenses.sync.GenericAccountService.Companion.getAccount
import org.totschnig.myexpenses.sync.SyncAdapter

class SyncNotificationDismissHandler : IntentService("SyncNotificationDismissHandler") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val accountName = intent.getStringExtra(KEY_SYNC_ACCOUNT_NAME)
            if (accountName != null) {
                val account = getAccount(accountName)
                SyncAdapter.clearNotificationContent(account)
            }
        }
    }
}